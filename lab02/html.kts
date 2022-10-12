#!/usr/bin/env kotlin
#
sealed interface Element {

    fun render(): String

    sealed interface Tag<out C : Element> : Element {

        val name: String get() = this::class.simpleName?.lowercase() ?: TODO()
        val children: List<C>
        val attributes: List<Attribute>

        interface Unique<out C : Element> : Tag<C>
        interface Repeatable<out C : Element> : Tag<C>

        override fun render() = """
            <$name ${attributes.joinToString(" "){(key, value) -> "$key=$\"$value\""}}>
            ${children.joinToString("") { it.render() }}
            </$name>
        """
    }
    data class Text(val content: String) : Element {
        override fun render() = content
    }
}

interface Attribute {
    val key: String 
    val value: String

    operator fun component1() = key
    operator fun component2() = value
}

typealias Tag<C> = Element.Tag<C>
typealias UniqueTag<C> = Element.Tag.Unique<C>
typealias RepeatableTag<C> = Element.Tag.Repeatable<C>
typealias Text = Element.Text

abstract class AbstractTag<C : Element> : Tag<C> {
    private var actualChildren: List<C> = emptyList()
    final override val children: List<C> get() = actualChildren
    final override val attributes: List<Attribute> = emptyList()

    internal fun appendChildren(child: C): Unit { actualChildren += child }
}

abstract class AbstractTagWithAnyChild() : AbstractTag<Element>() {
    operator fun String.unaryMinus() = apply { appendChildren(Text(this)) }
    operator fun String.unaryPlus() = apply { appendChildren(Text(this.capitalize())) }
}
abstract class AbstractTagWithTagChildren() : AbstractTag<Tag<Element>>()
abstract class BodyTag : AbstractTagWithAnyChild()

class Title(title: String) : UniqueTag<Text> {
    override val children: List<Text> = listOf(Text(title))
    override val attributes: List<Attribute> = emptyList()
}
class Head() : AbstractTag<Tag<Element>>(), UniqueTag<Tag<Element>>
class Body() : BodyTag(), UniqueTag<Element>
class HTML() : AbstractTagWithAnyChild(), UniqueTag<Element>

class P() : BodyTag(), RepeatableTag<Element>

data class A(val href: String) : BodyTag(), RepeatableTag<Element>


// p("class" to "pluto", "href" to "http:///sadaewdewdew") {
//     - "ciuao"
// }

// DSL

fun html(configuration: HTML.() -> Unit): HTML = HTML().apply { configuration() }
fun HTML.head(configuration: Head.() -> Unit): HTML = apply { appendChildren(Head().apply { configuration() }) }
fun HTML.body(configuration: Body.() -> Unit): HTML = apply { appendChildren(Body().apply { configuration() } ) }
fun Head.title(produceTitle: () -> String): Head = apply { appendChildren(Title(produceTitle())) }
fun BodyTag.p(configuration: P.() -> Unit): BodyTag = apply { appendChildren(P().apply { configuration() }) }

val result = html {
    head { // () -> Unit
        title { "Ciao" }
    }
    body {
        - "ciao ciao"
        + "pluto"
    }
}

println(result.render())
