/*
 * Copyright 2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.kotlin.dsl.execution

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.jetbrains.kotlin.lexer.KotlinLexer
import org.junit.Test


class AnnotationParserTest {

    // TODO: more test, cover typical annotations at least
    // TODO: everything that's a symbol("something") should be checked, most likely a token(something) needs to be used instead


    @Test
    fun `can parse single annotation`() {
        assertAnnotationConsumed("@Suppress(\"unused_variable\")")
        assertAnnotationConsumed("@Suppress ( \"unused_variable\" )")

        assertAnnotationConsumed("@DisableCachingByDefault(because=\"Not worth caching\")")
        assertAnnotationConsumed("@DisableCachingByDefault ( because = \"Not worth caching\" )")

        assertAnnotationConsumed("@Deprecated(message=\"Use rem(other) instead\",replaceWith=ReplaceWith(\"rem(other)\"))")
        assertAnnotationConsumed("@Deprecated ( message = \"Use rem(other) instead\" , replaceWith = ReplaceWith ( \"rem(other)\" ) )")

        assertAnnotationConsumed("@Throws(IOException::class)")
        assertAnnotationConsumed("@Throws ( IOException :: class )")

        assertAnnotationConsumed("@Throws(exceptionClasses=arrayOf(IOException::class,IllegalArgumentException::class))")
        assertAnnotationConsumed("@Throws ( exceptionClasses = arrayOf ( IOException :: class , IllegalArgumentException :: class ) )")

        assertAnnotationConsumed("@Throws(exceptionClasses=[IOException::class,IllegalArgumentException::class])")
        assertAnnotationConsumed("@Throws ( exceptionClasses = [ IOException :: class , IllegalArgumentException :: class ] )")
    }

    @Test
    fun `can parse multi annotation`() {
        // todo
    }

    private
    fun assertAnnotationConsumed(input: String) {
        val whitespace = "   "
        assertThat(
            annotationParser.consumeFrom(input + whitespace + "something more"),
            equalTo(input + whitespace)
        )
    }

    @Test
    fun `can parse file annotation`() {
        // todo
    }
}


private
fun <T> Parser<T>.consumeFrom(input: String): String {
    val kotlinLexer = KotlinLexer()
    val result = kotlinLexer.let { lexer ->
        lexer.start(input)
        this(lexer)
    }
    if (result is ParserResult.Failure) {
        throw Exception(result.reason)
    }
    return input.substring(0, kotlinLexer.currentPosition.offset)
}
