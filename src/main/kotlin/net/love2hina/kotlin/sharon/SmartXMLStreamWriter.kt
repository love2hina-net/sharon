package net.love2hina.kotlin.sharon

import java.io.Closeable
import java.io.File
import java.io.Writer
import javax.xml.namespace.NamespaceContext
import javax.xml.stream.XMLOutputFactory
import javax.xml.stream.XMLStreamWriter

class SmartXMLStreamWriter(file: File): XMLStreamWriter, Closeable {

    private val fileWriter: Writer

    private val xmlWriter: XMLStreamWriter

    private val lineSeparator = System.lineSeparator()

    private var emptyElement: Boolean = false

    init {
        fileWriter = file.bufferedWriter(Charsets.UTF_8)
        xmlWriter = XMLOutputFactory.newInstance().createXMLStreamWriter(fileWriter)
    }

    private fun startElement() {
        emptyElement = true
    }

    private fun outputElementBody() {
        xmlWriter.writeCharacters(lineSeparator)
        emptyElement = false
    }

    private fun endElement() {
        if (!emptyElement) {
            xmlWriter.writeCharacters(lineSeparator)
        }
        emptyElement = false
    }

    override fun writeStartDocument() {
        xmlWriter.writeStartDocument()
        startElement()
    }

    override fun writeStartDocument(version: String?) {
        xmlWriter.writeStartDocument(version)
        startElement()
    }

    override fun writeStartDocument(encoding: String?, version: String?) {
        xmlWriter.writeStartDocument(encoding, version)
        startElement()
    }

    override fun writeEndDocument() {
        endElement()
        xmlWriter.writeEndDocument()
    }

    override fun writeProcessingInstruction(target: String?) {
        outputElementBody()
        xmlWriter.writeProcessingInstruction(target)
    }

    override fun writeProcessingInstruction(target: String?, data: String?) {
        outputElementBody()
        xmlWriter.writeProcessingInstruction(target, data)
    }

    override fun writeDTD(dtd: String?) {
        outputElementBody()
        xmlWriter.writeDTD(dtd)
    }

    override fun writeCData(data: String?) {
        outputElementBody()
        xmlWriter.writeCData(data)
    }

    override fun writeEntityRef(name: String?) {
        outputElementBody()
        xmlWriter.writeEntityRef(name)
    }

    override fun writeEmptyElement(localName: String?) {
        outputElementBody()
        xmlWriter.writeEmptyElement(localName)
    }

    override fun writeEmptyElement(namespaceURI: String?, localName: String?) {
        outputElementBody()
        xmlWriter.writeEmptyElement(namespaceURI, localName)
    }

    override fun writeEmptyElement(prefix: String?, localName: String?, namespaceURI: String?) {
        outputElementBody()
        xmlWriter.writeEmptyElement(prefix, localName, namespaceURI)
    }

    override fun writeStartElement(localName: String?) {
        outputElementBody()
        xmlWriter.writeStartElement(localName)
        startElement()
    }

    override fun writeStartElement(namespaceURI: String?, localName: String?) {
        outputElementBody()
        xmlWriter.writeStartElement(namespaceURI, localName)
        startElement()
    }

    override fun writeStartElement(prefix: String?, localName: String?, namespaceURI: String?) {
        outputElementBody()
        xmlWriter.writeStartElement(prefix, localName, namespaceURI)
        startElement()
    }

    override fun writeEndElement() {
        endElement()
        xmlWriter.writeEndElement()
    }

    override fun writeDefaultNamespace(namespaceURI: String?) {
        xmlWriter.writeDefaultNamespace(namespaceURI)
    }

    override fun writeNamespace(prefix: String?, namespaceURI: String?) {
        xmlWriter.writeNamespace(prefix, namespaceURI)
    }

    override fun writeAttribute(localName: String?, value: String?) {
        xmlWriter.writeAttribute(localName, value)
    }

    override fun writeAttribute(namespaceURI: String?, localName: String?, value: String?) {
        xmlWriter.writeAttribute(namespaceURI, localName, value)
    }

    override fun writeAttribute(prefix: String?, namespaceURI: String?, localName: String?, value: String?) {
        xmlWriter.writeAttribute(prefix, namespaceURI, localName, value)
    }

    override fun writeComment(data: String?) {
        outputElementBody()
        xmlWriter.writeComment(data)
    }

    override fun writeCharacters(text: String?) {
        outputElementBody()
        xmlWriter.writeCharacters(text)
    }

    override fun writeCharacters(text: CharArray?, start: Int, len: Int) {
        outputElementBody()
        xmlWriter.writeCharacters(text, start, len)
    }

    override fun setDefaultNamespace(uri: String?) {
        xmlWriter.setDefaultNamespace(uri)
    }

    override fun getNamespaceContext(): NamespaceContext = xmlWriter.namespaceContext

    override fun setNamespaceContext(context: NamespaceContext?) {
        xmlWriter.namespaceContext = context
    }

    override fun getPrefix(uri: String?): String {
        return xmlWriter.getPrefix(uri)
    }

    override fun setPrefix(prefix: String?, uri: String?) {
        xmlWriter.setPrefix(prefix, uri)
    }

    override fun getProperty(name: String?): Any {
        return xmlWriter.getProperty(name)
    }

    override fun flush() {
        xmlWriter.flush()
        fileWriter.flush()
    }

    override fun close() {
        xmlWriter.close()
        fileWriter.close()
    }

}
