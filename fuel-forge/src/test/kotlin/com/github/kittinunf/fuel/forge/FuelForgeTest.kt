package com.github.kittinunf.fuel.forge

import com.github.kittinunf.forge.core.JSON
import com.github.kittinunf.forge.core.apply
import com.github.kittinunf.forge.core.at
import com.github.kittinunf.forge.core.map
import com.github.kittinunf.forge.core.maybeAt
import com.github.kittinunf.forge.util.create
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.test.MockHttpTestCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.hamcrest.CoreMatchers.isA
import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThat
import org.junit.Test
import java.net.HttpURLConnection

// TODO: Needs to Test on awaitResponseResultObject()
class FuelForgeTest : MockHttpTestCase() {
    data class IssueInfo(val id: Int, val title: String, val number: Int?)

    private val issueInfoDeserializer = { json: JSON ->
        ::IssueInfo.create
            .map(json at "id")
            .apply(json at "title")
            .apply(json maybeAt "number")
    }

    @Test
    fun `multiple issues objects with size and class`() = runBlocking {
        mock.chain(
            request = mock.request().withPath("/issues"),
            response = mock.response().withBody("[ " +
                "{ \"id\": 1, \"title\": \"issue 1\", \"number\": null }, " +
                "{ \"id\": 2, \"title\": \"issue 2\", \"number\": 32 }, " +
                " ]").withStatusCode(HttpURLConnection.HTTP_OK)
        )

        val (_, result) = withContext(Dispatchers.IO) {
            Fuel.get(mock.path("issues")).awaitResponseResultObjects(issueInfoDeserializer)
        }
        val issues = result.get()
        assertEquals(issues.size, 2)
        assertThat(issues[0], isA(IssueInfo::class.java))
    }

    @Test
    fun `multiple issues objects for not null and size`() = runBlocking {
        mock.chain(
            request = mock.request().withPath("/issues"),
            response = mock.response().withBody("[ " +
                "{ \"id\": 1, \"title\": \"issue 1\", \"number\": null }, " +
                "{ \"id\": 2, \"title\": \"issue 2\", \"number\": 32 }, " +
                " ]").withStatusCode(HttpURLConnection.HTTP_OK)
        )

        val (response, result) = withContext(Dispatchers.IO) {
            Fuel.get(mock.path("issues")).awaitResponseResultObjects(issueInfoDeserializer)
        }
        assertNotNull(response)
        assertNotNull(result.component1())
        assertEquals(result.component1()?.size, 2)
        assertNull(result.component2())
    }

    @Test
    fun `check forgeDeserializerOf is equal to 123`() {
       val content = """ { "id": 123, "title": "title1", "number": 1 } """
       val issue = forgeDeserializerOf(issueInfoDeserializer).deserialize(content)
       assertNotNull(issue)
       assertEquals(issue?.id, 123)
    }

    @Test(expected = JSONException::class)
    fun `invalid forge items with expected errors`() {
        val content = """ [
            { "id": 123, "title": "title1", "number": 1 },
            { "id": 456, "title": "title2" }
        ] """

        forgeDeserializerOf(issueInfoDeserializer).deserialize(content)
    }

    @Test
    fun `check forge items with size and first id`() {
       val content = """ [
            { "id": 123, "title": "title1", "number": 1 },
            { "id": 456, "title": "title2" }
        ] """

        val issues = forgesDeserializerOf(issueInfoDeserializer).deserialize(content)
        assertNotNull(issues)
        assertEquals(issues?.size, 2)
        assertEquals(issues?.first()?.id, 123)
    }
}