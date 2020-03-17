@file:Suppress("INTERFACE_WITH_SUPERCLASS", "OVERRIDING_FINAL_MEMBER", "RETURN_TYPE_MISMATCH_ON_OVERRIDE", "CONFLICTING_OVERLOADS", "EXTERNAL_DELEGATION")

package com.supercilex.robotscouter.server.utils.types

import org.w3c.dom.Plugin
import org.w3c.xhr.XMLHttpRequest
import kotlin.js.Json
import kotlin.js.Promise

typealias CallbackHandler = (err: Any?, res: Response) -> Unit

typealias Serializer = (obj: Any) -> String

typealias BrowserParser = (str: String) -> Any

typealias NodeParser = (res: Response, callback: (err: Error?, body: Any) -> Unit) -> Unit

external interface SuperAgentRequest : Request {
    var cookies: String
    var method: String
    var url: String
}

external interface SuperAgentStatic : SuperAgent<SuperAgentRequest> {
    fun agent(): SuperAgentStatic /* this */
}

external interface SuperAgent<Req : SuperAgentRequest> {
    fun attachCookies(req: Req)
    fun checkout(url: String, callback: CallbackHandler? = definedExternally): Req
    fun connect(url: String, callback: CallbackHandler? = definedExternally): Req
    fun copy(url: String, callback: CallbackHandler? = definedExternally): Req
    fun del(url: String, callback: CallbackHandler? = definedExternally): Req
    fun delete(url: String, callback: CallbackHandler? = definedExternally): Req
    fun get(url: String, callback: CallbackHandler? = definedExternally): Req
    fun head(url: String, callback: CallbackHandler? = definedExternally): Req
    fun lock(url: String, callback: CallbackHandler? = definedExternally): Req
    fun merge(url: String, callback: CallbackHandler? = definedExternally): Req
    fun mkactivity(url: String, callback: CallbackHandler? = definedExternally): Req
    fun mkcol(url: String, callback: CallbackHandler? = definedExternally): Req
    fun move(url: String, callback: CallbackHandler? = definedExternally): Req
    fun notify(url: String, callback: CallbackHandler? = definedExternally): Req
    fun options(url: String, callback: CallbackHandler? = definedExternally): Req
    fun patch(url: String, callback: CallbackHandler? = definedExternally): Req
    fun post(url: String, callback: CallbackHandler? = definedExternally): Req
    fun propfind(url: String, callback: CallbackHandler? = definedExternally): Req
    fun proppatch(url: String, callback: CallbackHandler? = definedExternally): Req
    fun purge(url: String, callback: CallbackHandler? = definedExternally): Req
    fun put(url: String, callback: CallbackHandler? = definedExternally): Req
    fun report(url: String, callback: CallbackHandler? = definedExternally): Req
    fun saveCookies(res: Response)
    fun search(url: String, callback: CallbackHandler? = definedExternally): Req
    fun subscribe(url: String, callback: CallbackHandler? = definedExternally): Req
    fun trace(url: String, callback: CallbackHandler? = definedExternally): Req
    fun unlock(url: String, callback: CallbackHandler? = definedExternally): Req
    fun unsubscribe(url: String, callback: CallbackHandler? = definedExternally): Req
}

external interface Response {
    var accepted: Boolean
    var badRequest: Boolean
    var body: Json
    var charset: String
    var clientError: Boolean
    var error: Error
    var files: Any
    var forbidden: Boolean
    fun get(header: String): String
    fun get(header: String /* 'Set-Cookie' */): Array<String>
    var header: Any
    var info: Boolean
    var links: Any?
        get() = definedExternally
        set(value) = definedExternally
    var noContent: Boolean
    var notAcceptable: Boolean
    var notFound: Boolean
    var ok: Boolean
    var redirect: Boolean
    var serverError: Boolean
    var status: Number
    var statusType: Number
    var text: String
    var type: String
    var unauthorized: Boolean
    var xhr: XMLHttpRequest
    var redirects: Array<String>
}

external interface `T$2` {
    var type: dynamic /* 'basic' | 'auto' */
        get() = definedExternally
        set(value) = definedExternally
}

external interface `T$3` {
    var type: String /* 'bearer' */
}

external interface `T$4` {
    var deadline: Number?
        get() = definedExternally
        set(value) = definedExternally
    var response: Number?
        get() = definedExternally
        set(value) = definedExternally
}

external interface Request : Promise<Response> {
    fun abort()
    fun accept(type: String): Request /* this */
    fun auth(user: String, pass: String, options: `T$2` = definedExternally): Request /* this */
    fun auth(token: String, options: `T$3`): Request /* this */
    fun buffer(`val`: Boolean? = definedExternally): Request /* this */
    fun clearTimeout(): Request /* this */
    fun end(callback: CallbackHandler? = definedExternally)
    fun get(field: String): String
    fun ok(callback: (res: Response) -> Boolean): Request /* this */
    fun on(name: String /* 'error' */, handler: (err: Any) -> Unit): Request /* this */
    fun on(name: String /* 'progress' */, handler: (event: ProgressEvent) -> Unit): Request /* this */
    fun on(name: String /* 'response' */, handler: (response: Response) -> Unit): Request /* this */
    fun on(name: String, handler: (event: Any) -> Unit): Request /* this */
    fun parse(parser: BrowserParser): Request /* this */
    fun parse(parser: NodeParser): Request /* this */
    fun part(): Request /* this */
    fun query(`val`: Any?): Request /* this */
    fun query(`val`: String): Request /* this */
    fun redirects(n: Number): Request /* this */
    fun responseType(type: String): Request /* this */
    fun retry(count: Number? = definedExternally, callback: CallbackHandler? = definedExternally): Request /* this */
    fun send(data: String? = definedExternally): Request /* this */
    fun send(data: Any? = definedExternally): Request /* this */
    fun serialize(serializer: Serializer): Request /* this */
    fun set(field: Any?): Request /* this */
    fun set(field: String, `val`: String): Request /* this */
    fun set(field: String /* 'Cookie' */, `val`: Array<String>): Request /* this */
    fun field(field: String, `val`: String): Request /* this */
    fun timeout(ms: Number): Request /* this */
    fun timeout(ms: `T$4`): Request /* this */
    fun type(`val`: String): Request /* this */
    fun unset(field: String): Request /* this */
    fun use(fn: Plugin): Request /* this */
    fun withCredentials(): Request /* this */
    fun maxResponseSize(size: Number): Request /* this */
    fun send(): Request /* this */
}

external interface ProgressEvent {
    var direction: dynamic /* 'download' | 'upload' */
        get() = definedExternally
        set(value) = definedExternally
    var loaded: Number
    var percent: Number?
        get() = definedExternally
        set(value) = definedExternally
    var total: Number?
        get() = definedExternally
        set(value) = definedExternally
}
