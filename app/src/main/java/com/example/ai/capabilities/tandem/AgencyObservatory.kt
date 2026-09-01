package com.example.ai.capabilities.tandem

interface AgencyObservatory {
    fun broadcast(entry: MindstreamEntry)
    fun getRecentStream(limit: Int = 10): List<MindstreamEntry>
}

class AgencyObservatoryImpl : AgencyObservatory {
    private val stream = mutableListOf<MindstreamEntry>()
    
    override fun broadcast(entry: MindstreamEntry) {
        stream.add(entry)
        // In a real Android app, this would emit to a Flow/LiveData collected by the UI
    }
    
    override fun getRecentStream(limit: Int): List<MindstreamEntry> {
        return stream.takeLast(limit)
    }
}
