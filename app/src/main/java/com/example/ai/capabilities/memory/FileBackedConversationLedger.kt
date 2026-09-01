package com.example.ai.capabilities.memory

import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.FileInputStream
import java.io.FileOutputStream

class FileBackedConversationLedger(private val file: File) : ImmutableConversationLedger {
    private var ledger = mutableListOf<ConversationEvent>()
    private var indexById = mutableMapOf<String, ConversationEvent>()

    init {
        if (file.exists() && file.length() > 0) {
            loadFromFile()
        }
    }

    @Synchronized
    override fun appendEvent(event: ConversationEvent) {
        ledger.add(event)
        indexById[event.eventId] = event
        saveToFile()
    }

    @Synchronized
    override fun getEvent(eventId: String): ConversationEvent? = indexById[eventId]

    @Synchronized
    override fun queryEventsByTime(startTime: Long, endTime: Long): List<ConversationEvent> {
        return ledger.filter { it.timestamp in startTime..endTime }
    }

    @Synchronized
    override fun getProvenanceChain(eventId: String): List<ConversationEvent> {
        val chain = mutableListOf<ConversationEvent>()
        var current = getEvent(eventId)
        while (current != null) {
            chain.add(current)
            current = current.supersededByEventId?.let { getEvent(it) }
        }
        return chain
    }

    private fun loadFromFile() {
        ObjectInputStream(FileInputStream(file)).use { stream ->
            @Suppress("UNCHECKED_CAST")
            ledger = stream.readObject() as MutableList<ConversationEvent>
            indexById = ledger.associateBy { it.eventId }.toMutableMap()
        }
    }

    private fun saveToFile() {
        ObjectOutputStream(FileOutputStream(file)).use { stream ->
            stream.writeObject(ledger)
        }
    }
}
