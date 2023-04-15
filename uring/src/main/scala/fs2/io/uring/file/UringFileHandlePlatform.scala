/*
 * Copyright (c) 2013 Functional Streams for Scala
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package fs2.io.uring.file

import java.nio.ByteBuffer
import java.nio.channels.{FileChannel, FileLock}

import cats.effect.kernel.{Async, Resource, Sync}
import fs2.Chunk
import fs2.io.file.Flags
import fs2.io.file.Path
import cats.syntax.all._
import fs2.io.uring._
import fs2.io.uring.unsafe.uring._
import fs2.io.uring.unsafe.util._
import scala.scalanative.unsigned._
import scala.scalanative.unsafe._

private[file] trait UringFileHandlePlatform[F[_]] {
  // some apis those are specific to io_uring.
}

private[file] trait UringFileHandleCompanionPlatform {
  def fromPath[F[_]: Async](path: Path, flags: Flags): Resource[F, UringFileHandle[F]] =
    UringFiles[F].open(path, flags)

  def fromFileChannel[F[_]: Async](channel: F[FileChannel]): Resource[F, UringFileHandle[F]] = ???

  /** Creates a `UringFileHandle[F]` from a `java.nio.channels.FileChannel`. */
  private[file] def make[F[_]](
      chan: FileChannel,
      ring: Uring[F],
      fd: Int
  )(implicit F: Sync[F]): UringFileHandle[F] =
    new UringFileHandle[F] {
      type Lock = FileLock

      override def force(metaData: Boolean): F[Unit] =
        F.blocking(chan.force(metaData))

      private def readFixed(
          bytes: Ptr[Byte],
          maxBytes: Int,
          offset: Int,
          fixedBufferIndex: Int
      ): F[Int] =
        ring.call(
          io_uring_prep_read_fixed(_, fd, bytes, maxBytes.toULong, offset.toULong, fixedBufferIndex)
        )

      override def read(numBytes: Int, offset: Long): F[Option[Chunk[Byte]]] = {
        for {
          buf <- ring.popFixedBuffer
          _ <- readFixed(buf._1, numBytes, offset.toInt, buf._2)
        } yield Sync[F].delay(None)

        ???
      }

      override def size: F[Long] =
        F.blocking(chan.size)

      override def truncate(size: Long): F[Unit] =
        F.blocking { chan.truncate(size); () }

      override def write(bytes: Chunk[Byte], offset: Long): F[Int] =
        F.blocking(chan.write(bytes.toByteBuffer, offset))
    }
}
