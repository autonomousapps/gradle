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

import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.lexer.KtTokens.ANDAND
import org.jetbrains.kotlin.lexer.KtTokens.ARROW
import org.jetbrains.kotlin.lexer.KtTokens.AT
import org.jetbrains.kotlin.lexer.KtTokens.CLASS_KEYWORD
import org.jetbrains.kotlin.lexer.KtTokens.COLON
import org.jetbrains.kotlin.lexer.KtTokens.COLONCOLON
import org.jetbrains.kotlin.lexer.KtTokens.COMMA
import org.jetbrains.kotlin.lexer.KtTokens.DIV
import org.jetbrains.kotlin.lexer.KtTokens.DIVEQ
import org.jetbrains.kotlin.lexer.KtTokens.DOT
import org.jetbrains.kotlin.lexer.KtTokens.ELVIS
import org.jetbrains.kotlin.lexer.KtTokens.EQ
import org.jetbrains.kotlin.lexer.KtTokens.EQEQ
import org.jetbrains.kotlin.lexer.KtTokens.EQEQEQ
import org.jetbrains.kotlin.lexer.KtTokens.EXCL
import org.jetbrains.kotlin.lexer.KtTokens.EXCLEQ
import org.jetbrains.kotlin.lexer.KtTokens.EXCLEQEQEQ
import org.jetbrains.kotlin.lexer.KtTokens.GT
import org.jetbrains.kotlin.lexer.KtTokens.GTEQ
import org.jetbrains.kotlin.lexer.KtTokens.IN_KEYWORD
import org.jetbrains.kotlin.lexer.KtTokens.IS_KEYWORD
import org.jetbrains.kotlin.lexer.KtTokens.LBRACKET
import org.jetbrains.kotlin.lexer.KtTokens.LT
import org.jetbrains.kotlin.lexer.KtTokens.LTEQ
import org.jetbrains.kotlin.lexer.KtTokens.MINUS
import org.jetbrains.kotlin.lexer.KtTokens.MINUSEQ
import org.jetbrains.kotlin.lexer.KtTokens.MINUSMINUS
import org.jetbrains.kotlin.lexer.KtTokens.MUL
import org.jetbrains.kotlin.lexer.KtTokens.MULTEQ
import org.jetbrains.kotlin.lexer.KtTokens.NOT_IN
import org.jetbrains.kotlin.lexer.KtTokens.NOT_IS
import org.jetbrains.kotlin.lexer.KtTokens.NULL_KEYWORD
import org.jetbrains.kotlin.lexer.KtTokens.OROR
import org.jetbrains.kotlin.lexer.KtTokens.OUT_KEYWORD
import org.jetbrains.kotlin.lexer.KtTokens.PERC
import org.jetbrains.kotlin.lexer.KtTokens.PERCEQ
import org.jetbrains.kotlin.lexer.KtTokens.PLUS
import org.jetbrains.kotlin.lexer.KtTokens.PLUSEQ
import org.jetbrains.kotlin.lexer.KtTokens.PLUSPLUS
import org.jetbrains.kotlin.lexer.KtTokens.QUEST
import org.jetbrains.kotlin.lexer.KtTokens.RANGE
import org.jetbrains.kotlin.lexer.KtTokens.RBRACKET
import org.jetbrains.kotlin.lexer.KtTokens.SAFE_ACCESS
import org.jetbrains.kotlin.lexer.KtTokens.SEMICOLON
import org.jetbrains.kotlin.lexer.KtTokens.THIS_KEYWORD


private
class AnnotationParser {

    // todo: re-check all the definitions


    private
    val debugger: ParserDebugger = ParserDebugger()

    private
    fun <T> debug(name: String, parser: Parser<T>) = debugger.debug(name, parser) // todo: remove or disable


    // not implemented parsers // todo: can we afford doing this?
    private
    val classDeclaration: Parser<Any> = { failure("classDeclaration not implemented") }

    private
    val objectDeclaration: Parser<Any> = { failure("objectDeclaration not implemented") }

    private
    val functionDeclaration: Parser<Any> = { failure("functionDeclaration not implemented") }

    private
    val propertyDeclaration: Parser<Any> = { failure("propertyDeclaration not implemented") }

    private
    val typeAlias: Parser<Any> = { failure("typeAlias not implemented") }

    private
    val loopStatement: Parser<Any> = { failure("loopStatement not implemented") }

    private
    val tryExpression: Parser<Any> = { failure("tryExpression not implemented") }

    private
    val jumpExpression: Parser<Any> = { failure("jumpExpression not implemented") }

    private
    val objectLiteral: Parser<Any> = { failure("objectLiteral not implemented") }

    private
    val superExpression: Parser<Any> = { failure("superExpression not implemented") }

    private
    val anonymousFunction: Parser<Any> = { failure("anonymousFunction not implemented") }

    private
    val ifExpression: Parser<Any> = { failure("ifExpression not implemented") }

    private
    val whenExpression: Parser<Any> = { failure("whenExpression not implemented") }


    // basic building blocks
    private
    val simpleIdentifier =
        symbol()

    private
    val thisExpression =
        token(THIS_KEYWORD) * optional(token(AT) * simpleIdentifier)

    private
    val label =
        simpleIdentifier * token(AT)

    private
    val asOperator =
        token(KtTokens.AS_KEYWORD) + token(KtTokens.AS_SAFE)

    private
    val additiveOperator =
        token(PLUS) + token(MINUS)

    private
    val varianceModifier =
        token(IN_KEYWORD) + token(OUT_KEYWORD)

    private
    val inOperator =
        token(IN_KEYWORD) + token(NOT_IN)

    private
    val isOperator =
        token(IS_KEYWORD) + token(NOT_IS)

    private
    val comparisonOperator =
        token(LT) + token(GT) + token(LTEQ) + token(GTEQ)

    private
    val equalityOperator =
        token(EXCLEQ) + token(EXCLEQEQEQ) + token(EQEQ) + token(EQEQEQ)

    private
    val prefixUnaryOperator =
        token(PLUSPLUS) + token(MINUSMINUS) + token(MINUS) + token(PLUS) + token(EXCL)

    private
    val multiplicativeOperator =
        token(MUL) + token(DIV) + token(PERC)

    private
    val memberAccessOperator =
        token(DOT) + token(SAFE_ACCESS) + token(COLONCOLON)

    private
    val semi =
        token(SEMICOLON)

    private
    val semis =
        oneOrMore(semi)

    private
    val assignmentAndOperator =
        token(PLUSEQ) + token(MINUSEQ) + token(MULTEQ) + token(DIVEQ) + token(PERCEQ)


    private
    val literalConstant =
        booleanLiteral + integerLiteral + floatLiteral + characterLiteral + token(NULL_KEYWORD)


    // recursive parsers
    private
    var type by recursive<Any>()

    private
    var annotation by recursive<Any>()

    private
    var parenthesizedUserType by recursive<Any>()

    private
    var expression by recursive<Any>()

    private
    var primaryExpression by recursive<Any?>()

    private
    var directlyAssignableExpression by recursive<Any>()

    private
    var assignableExpression by recursive<Any>()


    // regular parsers
    private
    val collectionLiteral =
        bracket(
            optional(
                expression * zeroOrMore(token(COMMA) * expression) * optional(token(COMMA))
            )
        )

    private
    val declaration =
        classDeclaration + objectDeclaration + functionDeclaration + propertyDeclaration + typeAlias

    private
    val parenthesizedType = paren(type)

    private
    val parenthesizedExpression =
        paren(expression)

    private
    val valueArgument =
        debug(
            "valueArgument",
            optional(annotation) *
                debug(
                    "optional(simpleIdentifier * token(EQ))",
                    optional(simpleIdentifier * token(EQ))
                ) *
                optional(token(MUL)) *
                expression
        )

    private
    val valueArguments =
        debug(
            "valueArguments",
            paren(
                optional(
                    valueArgument * zeroOrMore(token(COMMA) * valueArgument) * optional(token(COMMA))
                )
            )
        )

    private
    val variableDeclaration =
        zeroOrMore(annotation) *
            simpleIdentifier *
            optional(token(COMMA) * type)

    private
    val multiVariableDeclaration =
        paren(
            variableDeclaration *
                zeroOrMore(token(COMMA) * variableDeclaration) *
                optional(token(COMMA))
        )

    private
    val typeModifier =
        annotation + symbol("suspend") + token(KtTokens.SUSPEND_KEYWORD)

    private
    val typeModifiers =
        oneOrMore(typeModifier)

    private
    val typeProjectionModifier =
        varianceModifier + annotation

    private
    val typeProjectionModifiers =
        oneOrMore(typeProjectionModifier)

    private
    val typeProjection =
        optional(typeProjectionModifiers) * type + token(MUL)

    private
    val typeArguments =
        token(LT) *
            typeProjection *
            zeroOrMore(token(COMMA) * typeProjection) *
            optional(token(COMMA)) *
            token(GT)

    private
    val simpleUserType =
        debug(
            "simpleUserType",
            simpleIdentifier * optional(typeArguments)
        )

    private
    val userType =
        debug(
            "userType",
            simpleUserType * zeroOrMore(token(DOT) * simpleUserType)
        )

    private
    val unaryPrefix =
        debug(
            "unaryPrefix",
            annotation + label + prefixUnaryOperator
        )

    private
    val lambdaParameter =
        variableDeclaration +
            (multiVariableDeclaration * optional(token(COLON) * type))

    private
    val lambdaParameters =
        lambdaParameter *
            zeroOrMore(token(COMMA) * lambdaParameter) *
            optional(token(COMMA))

    private
    val assignment =
        debug(
            "assignment",
            ((directlyAssignableExpression * token(EQ)) + (assignableExpression * assignmentAndOperator)) * expression
        )

    private
    val statement =
        debug(
            "statement",
            zeroOrMore(label + annotation) *
                (declaration + assignment + loopStatement + expression)
        )

    private
    val statements: Parser<Any> =
        debug(
            "statements",
            optional(statement * zeroOrMore(semis * statement)) * optional(semis)
        )

    private
    val lambdaLiteral =
        brace(
            (optional(lambdaParameters) *
                optional(token(ARROW))) *
                statements
        )

    private
    val annotatedLambda =
        zeroOrMore(annotation) *
            optional(label) *
            lambdaLiteral

    private
    val callSuffix =
        optional(typeArguments) *
            ((optional(valueArguments) * annotatedLambda) + valueArguments)

    private
    val indexingSuffix =
        bracket(
            expression *
                zeroOrMore(token(COMMA) * expression * optional(token(COMMA)))
        )

    private
    val navigationSuffix: Parser<Any> =
        debug(
            "navigationSuffix",
            memberAccessOperator * (simpleIdentifier + parenthesizedExpression + token(CLASS_KEYWORD))
        )

    private
    val postfixUnarySuffix =
        typeArguments + callSuffix + indexingSuffix + navigationSuffix

    private
    val postfixUnaryExpression =
        debug(
            "postfixUnaryExpression",
            primaryExpression *
                debug(
                    "zeroOrMore(postfixUnarySuffix)",
                    zeroOrMore(postfixUnarySuffix)
                )
        )

    private
    val prefixUnaryExpression =
        debug(
            "prefixUnaryExpression",
            zeroOrMore(unaryPrefix) *
                postfixUnaryExpression
        )

    private
    val asExpression =
        debug(
            "asExpression",
            prefixUnaryExpression *
                zeroOrMore(asOperator * type)
        )

    private
    val multiplicativeExpression =
        debug(
            "multiplicativeExpression",
            asExpression *
                zeroOrMore(multiplicativeOperator * asExpression)
        )

    private
    val additiveExpression =
        debug(
            "additiveExpression",
            multiplicativeExpression * zeroOrMore(additiveOperator * multiplicativeExpression)
        )

    private
    val rangeExpression =
        debug(
            "rangeExpression",
            additiveExpression * zeroOrMore(token(RANGE) + additiveExpression)
        )

    private
    val infixFunctionCall =
        debug(
            "infixFunctionCall",
            rangeExpression * zeroOrMore(simpleIdentifier * rangeExpression)
        )

    private
    val elvisExpression =
        debug(
            "elvisExpression",
            infixFunctionCall * zeroOrMore(token(ELVIS) * infixFunctionCall)
        )

    private
    val infixOperation =
        debug(
            "infixOperation",
            elvisExpression * zeroOrMore((inOperator * elvisExpression) + (isOperator * type))
        )

    private
    val genericCallLikeComparison =
        debug(
            "genericCallLikeComparison",
            infixOperation * zeroOrMore(callSuffix)
        )

    private
    val comparison =
        debug(
            "comparison",
            genericCallLikeComparison * zeroOrMore(comparisonOperator * genericCallLikeComparison)
        )

    private
    val equality =
        debug(
            "equality",
            comparison * zeroOrMore(equalityOperator * comparison)
        )

    private
    val conjunction =
        debug(
            "conjunction",
            equality * zeroOrMore(token(ANDAND) * equality)
        )

    private
    val disjunction =
        conjunction * zeroOrMore(token(OROR) * conjunction)


    private
    val definitelyNonNullableType =
        optional(typeModifiers) *
            (userType + parenthesizedUserType) *
            token(MUL) *
            optional(typeModifiers) *
            (userType + parenthesizedUserType)

    private
    val constructorInvocation =
        debug(
            "constructorInvocation",
            userType * valueArguments
        )

    private
    val unescapedAnnotation =
        debug("unescapedAnnotation",
            constructorInvocation + userType
        )

    private
    val listOfUnescapedAnnotations =
        token(LBRACKET) * oneOrMore(unescapedAnnotation) * token(RBRACKET)

    private
    val annotationUseSiteTarget = symbol("field") + symbol("property") + symbol("get") + symbol("set") + symbol("receiver") + symbol("param") + symbol("setparam") + symbol("delgate")

    private
    val singleAnnotation =
        debug(
            "singleAnnotation",
            token(AT) * optionalX(annotationUseSiteTarget) * unescapedAnnotation
        )

    private
    val multiAnnotation = token(AT) * optional(annotationUseSiteTarget) * listOfUnescapedAnnotations

    private
    val parameter =
        simpleIdentifier * token(COLON) * type

    private
    val functionTypeParameters =
        paren(
            optional(parameter + type) *
                zeroOrMore(token(COMMA) * (parameter + type)) *
                optional(token(COMMA))
        )

    private
    val typeReference =
        userType + symbol("dynamic")


    private
    val nullableType =
        (typeReference + parenthesizedType) * oneOrMore(token(QUEST))

    private
    val receiverType =
        debug(
            "receiverType",
            optional(typeModifiers) * (parenthesizedType + nullableType + typeReference)
        )

    private
    val functionType =
        optional(receiverType * token(DOT)) * functionTypeParameters * token(ARROW) * type

    private
    val callableReference =
        debug(
            "callableReference",
            optional(receiverType) *
                debug(
                    "token(COLONCOLON)",
                    token(COLONCOLON)
                ) *
                debug(
                    "(simpleIdentifier + token(CLASS_KEYWORD))",
                    (simpleIdentifier + token(CLASS_KEYWORD))
                )
        )

    private
    val functionLiteral =
        lambdaLiteral + anonymousFunction

    private
    val assignableSuffix =
        debug(
            "assignableSuffix",
            typeArguments + indexingSuffix + navigationSuffix
        )


    init {
        type = optional(typeModifiers) * (functionType + parenthesizedType + nullableType + typeReference + definitelyNonNullableType)
        annotation = singleAnnotation + multiAnnotation
        parenthesizedUserType = paren(userType + parenthesizedUserType)
        expression =
            debug(
                "expression",
                disjunction
            )
        primaryExpression =
            debug("primaryExpression",
                parenthesizedExpression +
                    callableReference +
                    simpleIdentifier +
                    literalConstant +
                    stringLiteral +
                    functionLiteral +
                    objectLiteral +
                    collectionLiteral +
                    thisExpression +
                    superExpression +
                    ifExpression +
                    whenExpression +
                    tryExpression +
                    jumpExpression
            )
        directlyAssignableExpression =
            debug(
                "directlyAssignableExpression",
                (postfixUnaryExpression * assignableSuffix) +
                    simpleIdentifier +
                    paren(directlyAssignableExpression)
            )
        assignableExpression =
            debug(
                "assignableExpression",
                prefixUnaryExpression + paren(assignableExpression)
            )
    }


    fun annotationParser() = run {
        singleAnnotation + multiAnnotation
    }

    fun fileAnnotationParser() = run {
        token(AT) * symbol("file") * token(COLON) * (unescapedAnnotation + listOfUnescapedAnnotations)
    }
}


private
val parser = AnnotationParser()


internal
val annotationParser = parser.annotationParser()


internal
val fileAnnotationParser = parser.fileAnnotationParser()
