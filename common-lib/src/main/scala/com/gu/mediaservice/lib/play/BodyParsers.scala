package com.gu.mediaservice.lib.play

import java.io.{File, FileOutputStream}
import java.security.MessageDigest

import akka.stream.scaladsl.Sink
import akka.util.ByteString
import com.gu.mediaservice.lib.argo.ArgoHelpers
import play.api.Logger
import play.api.libs.streams.Accumulator
import play.api.mvc._

import scala.concurrent.ExecutionContext
import scala.util.{Either, Left, Right}

// TODO MRB: is this still required in Play 2.6?
// If so it should live in image-loader

case class DigestedFile(file: File, digest: String)

object DigestedFile {
  def apply(file: File, digest: Array[Byte]): DigestedFile =
    DigestedFile(file, digest.map("%02x".format(_)).mkString)
}

object DigestBodyParser extends ArgoHelpers {

  private val missingContentLengthError = respondError(
    Status(411),
    "missing-content-length",
    s"Missing content-length. Please specify a correct 'Content-Length' header"
  )

  private val incorrectContentLengthError = respondError(
    Status(400),
    "incorrect-content-length",
    s"Incorrect content-length. The specified content-length does match that of the received file."
  )

  def slurp(to: File)(implicit ec: ExecutionContext): Accumulator[ByteString, (MessageDigest, FileOutputStream)] = {
    Accumulator(Sink.fold[(MessageDigest, FileOutputStream), ByteString](
      (MessageDigest.getInstance("SHA-1"), new FileOutputStream(to))) {
      case ((md, os), data) =>
        md.update(data.toArray)
        os.write(data.toArray)

        (md, os)
    })
  }

  def failValidation(foo: Result, message: String) = {
    Logger.info(message)
    Left(foo)
  }

  def validate(request: RequestHeader, to: File, md: MessageDigest): Either[Result, DigestedFile] = {
    request.headers.get("Content-Length") match {
      case Some(contentLength) =>
        if (to.length == contentLength.toInt) Right(DigestedFile(to, md.digest))
        else failValidation(incorrectContentLengthError, "Received file does not match specified 'Content-Length'")
      case None =>
        failValidation(missingContentLengthError, "Missing content-length. Please specify a correct 'Content-Length' header")
    }
  }

  def create(to: File)(implicit ex: ExecutionContext): BodyParser[DigestedFile] =
    BodyParser("digested file, to=" + to) { request => {
      slurp(to).map { case (md, os) =>
        os.close()
        validate(request, to, md)
      }
    }
  }
}
