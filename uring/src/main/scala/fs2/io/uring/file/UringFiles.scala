/*
 * Copyright 2022 Arman Bilge
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fs2.io.uring.file

import cats.effect.kernel.{Async, Resource}

import fs2.Stream
import fs2.io.file.Path
import fs2.io.file._
import fs2.Pipe

sealed trait UringFiles[F[_]] extends UringFilesPlatform[F] {
  def open(path: Path, flags: Flags): Resource[F, UringFileHandle[F]]
  def readAll(path: Path): Stream[F, Byte] = readAll(path, 64 * 1024, Flags.Read)
  def readAll(path: Path, chunkSize: Int, flags: Flags): Stream[F, Byte]
  def writeAll(path: Path): Pipe[F, Byte, Nothing] = writeAll(path, Flags.Write)
  def writeAll(path: Path, flags: Flags): Pipe[F, Byte, Nothing]
}

object UringFiles extends UringFilesCompanionPlatform {
  private[fs2] abstract class UnsealedFiles[F[_]](implicit F: Async[F]) extends UringFiles[F] {}

  def apply[F[_]](implicit F: UringFiles[F]): UringFiles[F] = F
}
