package prowse

import java.time.{Instant, ZoneId, ZonedDateTime}

import spray.json.{DefaultJsonProtocol, _}

object BuildInfoHelper extends DefaultJsonProtocol {

    val buildDateTime: ZonedDateTime = Instant.parse(BuildInfo.buildInstant).atZone(ZoneId.of("GMT"))

    val buildInfoJson: String =
        Map(
            "name" -> BuildInfo.name,
            "version" -> BuildInfo.version,
            "gitChecksum" -> BuildInfo.gitChecksum,
            "buildInstant" -> BuildInfo.buildInstant
        ).toJson.compactPrint

}
