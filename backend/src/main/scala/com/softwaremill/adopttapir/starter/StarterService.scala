package com.softwaremill.adopttapir.starter

import cats.effect.IO
import com.softwaremill.adopttapir.logging.FLogging
import com.softwaremill.adopttapir.metrics.Metrics
import com.softwaremill.adopttapir.starter.files.FilesManager
import com.softwaremill.adopttapir.starter.formatting.GeneratedFilesFormatter
import com.softwaremill.adopttapir.template.ProjectGenerator

import java.io.File

class StarterService(generatedFilesFormatter: GeneratedFilesFormatter) extends FLogging:

  def generateZipFile(starterDetails: StarterDetails): IO[File] =
    logger.info(s"received request: $starterDetails") *>
      IO(generatedFilesFormatter.format(ProjectGenerator.generate(starterDetails)))
        .flatMap(formattedGeneratedFiles => {
          IO
            .blocking(generatedFilesFormatter.createTempDir())
            .bracket { tempDirectory =>
              for
                tempDir <- tempDirectory
                _ <- logger.debug("created temp dir: " + tempDir)
                generatedFiles <- formattedGeneratedFiles
                _ <- generatedFilesFormatter.createFiles(tempDir, generatedFiles)
                zippedFile <- generatedFilesFormatter.zipDirectory(tempDir)
                _ <- IO(Metrics.increaseZipGenerationMetricCounter(starterDetails))
              yield zippedFile
            }(release = tempDirectory => generatedFilesFormatter.deleteFilesAsStatedInConfig(tempDirectory))
        })
