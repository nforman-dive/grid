package lib

import com.gu.contentapi.client.GuardianContentClient
import com.gu.contentapi.client.model.v1.Content
import com.gu.contentapi.buildinfo.CapiBuildInfo

import com.ning.http.client.AsyncHttpClient
import com.ning.http.client.AsyncHttpClientConfig.Builder
import org.joda.time.{DateTime, DateTimeZone}
import dispatch.Http

trait ContentHelpers {
  def getContentFirstPublished(content: Content) = for {
    fields <- content.fields
    firstPublicationDate <- fields.firstPublicationDate
    date = new DateTime(firstPublicationDate.iso8601, DateTimeZone.UTC)
  } yield date

}

object LiveContentApi extends ContentApiRequestBuilder {
  override val targetUrl = Config.capiLiveUrl
}

class ContentApiRequestBuilder extends GuardianContentClient(apiKey = Config.capiApiKey) with ContentHelpers {
  override val userAgent = "content-api-scala-client/"+CapiBuildInfo.version

  val builder = new Builder()
    .setAllowPoolingConnections(true)
    .setMaxConnectionsPerHost(10)
    .setMaxConnections(10)
    .setConnectTimeout(1000)
    .setRequestTimeout(8000)
    .setCompressionEnforced(true)
    .setFollowRedirect(true)
    .setUserAgent(userAgent)
    .setConnectionTTL(60000)

  val client = new AsyncHttpClient(builder.build)

  override lazy val http = Http(client)
}

