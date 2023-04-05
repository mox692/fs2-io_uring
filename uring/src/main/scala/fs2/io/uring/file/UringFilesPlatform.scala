package fs2.io.uring.file

import cats.effect.kernel.{Async, Resource}
import fs2.{Stream, Pipe}
import fs2.io.file.Flags
import fs2.io.file.Path

private[file] trait UringFilesPlatform[F[_]] {}

private[file] trait UringFilesCompanionPlatform {
  implicit def forAsync[F[_]: Async]: UringFiles[F] = new AsyncUringFiles[F]

  private final class AsyncUringFiles[F[_]](protected implicit val F: Async[F])
      extends UringFiles.UnsealedFiles[F] {
    def open(path: Path, flags: Flags): Resource[F, UringFileHandle[F]] = ???
    def readAll(path: Path, chunkSize: Int, flags: Flags): Stream[F, Byte] = ???
    def writeAll(path: Path, flags: Flags): Pipe[F, Byte, Nothing] = ???
  }
}
