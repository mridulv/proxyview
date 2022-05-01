package com.proxyview.common.models

import rawhttp.core.body.EagerBodyReader
import rawhttp.core.{ EagerHttpResponse, HttpVersion, RawHttpHeaders, StatusLine }

import java.nio.charset.StandardCharsets.US_ASCII

object HttpResponses {

  def errorResponse: EagerHttpResponse[Void] = {
    val notFoundResponseBody = "Resource was not found.".getBytes(US_ASCII)
    val basicHeaders = RawHttpHeaders.newBuilderSkippingValidation.`with`("Content-Type", "text/plain").`with`("Cache-Control", "no-cache").`with`("Pragma", "no-cache").build
    val headers = RawHttpHeaders.newBuilderSkippingValidation(basicHeaders).overwrite("Content-Length", Integer.toString(notFoundResponseBody.length)).build
    new EagerHttpResponse[Void](null, null, new StatusLine(HttpVersion.HTTP_1_1, 404, "Not Found"), headers, new EagerBodyReader(notFoundResponseBody))
  }

}
