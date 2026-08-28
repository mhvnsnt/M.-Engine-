import React, { useState } from 'react';
import { Terminal, CheckCircle, Activity, Github, Shield, Layers, Box, Code } from 'lucide-react';

export default function App() {
  const [activeTab, setActiveTab] = useState('missions');
  const [input, setInput] = useState('');
  
  const bootstrapMission = {
    id: "miss-bootstrap-001",
    name: "Self-Audit & First Autonomous Web Update",
    status: "IN_PROGRESS",
    tasks: [
      { text: "Inspect M. Engine repository recursively via true JGit", done: false },
      { text: "Check reality limits (Web API, Remote Worker boundaries)", done: false },
      { text: "Fix highest-value blocker preventing full independent dev-loop", done: false },
      { text: "Provide REPRODUCTION_REGRESSION evidence", done: false }
    ]
  };

  return (
    <div className="flex h-screen w-full bg-gray-950 text-gray-200">
      {/* Sidebar */}
      <div className="w-64 border-r border-gray-800 bg-gray-900 flex flex-col">
        <div className="p-4 border-b border-gray-800 flex items-center gap-2">
          <Terminal className="text-blue-400" />
          <h1 className="font-bold text-lg tracking-wider">M. ENGINE</h1>
        </div>
        <nav className="flex-1 p-4 space-y-2">
          <button onClick={() => setActiveTab('missions')} className={`w-full flex items-center gap-3 px-3 py-2 rounded-md transition-colors ${activeTab === 'missions' ? 'bg-blue-900/30 text-blue-400' : 'hover:bg-gray-800'}`}>
            <Activity size={18} /> Missions
          </button>
          <button onClick={() => setActiveTab('connectors')} className={`w-full flex items-center gap-3 px-3 py-2 rounded-md transition-colors ${activeTab === 'connectors' ? 'bg-blue-900/30 text-blue-400' : 'hover:bg-gray-800'}`}>
            <Github size={18} /> Connectors
          </button>
          <button onClick={() => setActiveTab('evidence')} className={`w-full flex items-center gap-3 px-3 py-2 rounded-md transition-colors ${activeTab === 'evidence' ? 'bg-blue-900/30 text-blue-400' : 'hover:bg-gray-800'}`}>
            <Shield size={18} /> Evidence Ledger
          </button>
          <button onClick={() => setActiveTab('capabilities')} className={`w-full flex items-center gap-3 px-3 py-2 rounded-md transition-colors ${activeTab === 'capabilities' ? 'bg-blue-900/30 text-blue-400' : 'hover:bg-gray-800'}`}>
            <Layers size={18} /> Capability Graph
          </button>
        </nav>
        <div className="p-4 text-xs text-gray-500 border-t border-gray-800 flex justify-between">
          <span>Web Client v1.0.0</span>
          <span className="text-green-500 flex items-center gap-1"><span className="w-2 h-2 rounded-full bg-green-500 block"></span> Connected</span>
        </div>
      </div>

      {/* Main Content Area */}
      <div className="flex-1 flex flex-col bg-gray-950">
        <div className="flex-1 p-6 overflow-y-auto">
          {activeTab === 'missions' && (
            <div className="max-w-4xl mx-auto space-y-6">
              <h2 className="text-2xl font-semibold mb-6 flex items-center gap-2"><Activity /> Active Mission Loop</h2>
              
              <div className="bg-gray-900 border border-gray-800 rounded-lg p-6 shadow-xl">
                <div className="flex justify-between items-start mb-4">
                  <div>
                    <span className="text-xs font-mono text-blue-400 mb-1 block">{bootstrapMission.id}</span>
                    <h3 className="text-xl font-bold">{bootstrapMission.name}</h3>
                  </div>
                  <span className="px-3 py-1 bg-blue-900/40 text-blue-400 rounded-full text-xs font-bold border border-blue-800">
                    {bootstrapMission.status}
                  </span>
                </div>
                
                <div className="space-y-3 mt-6">
                  {bootstrapMission.tasks.map((task, i) => (
                    <div key={i} className="flex items-start gap-3 bg-gray-950 p-3 rounded border border-gray-800">
                      <CheckCircle size={18} className={task.done ? "text-green-400" : "text-gray-600"} />
                      <span className={task.done ? "text-gray-400 line-through" : "text-gray-200"}>{task.text}</span>
                    </div>
                  ))}
                </div>

                <div className="mt-6 p-4 bg-black rounded border border-gray-800 font-mono text-sm text-green-400 h-32 overflow-y-auto">
                  &gt; Executing Universal Reality Loop...<br/>
                  &gt; UNDERSTAND: Parsing mission objectives...<br/>
                  &gt; RETRIEVE: Checking capability graph for existing Web UI bounds...<br/>
                  &gt; Awaiting external worker delegation...
                </div>
              </div>
            </div>
          )}

          {activeTab === 'connectors' && (
            <div className="max-w-4xl mx-auto">
              <h2 className="text-2xl font-semibold mb-6 flex items-center gap-2"><Github /> First-Class Connectors</h2>
              <div className="bg-gray-900 border border-gray-800 rounded-lg p-6 flex items-center justify-between">
                <div className="flex items-center gap-4">
                  <Github size={32} />
                  <div>
                    <h3 className="font-bold text-lg">GitHub Application</h3>
                    <p className="text-gray-400 text-sm">OAuth / Delegated Installation Flow</p>
                  </div>
                </div>
                <button className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded font-medium transition-colors">
                  Authorize GitHub
                </button>
              </div>
            </div>
          )}
        </div>

        {/* Universal Command Center Input */}
        <div className="p-6 border-t border-gray-800 bg-gray-900">
          <div className="max-w-4xl mx-auto relative">
            <input 
              type="text" 
              value={input}
              onChange={(e) => setInput(e.target.value)}
              placeholder="Command M. Engine to inspect, build, or fix..." 
              className="w-full bg-gray-950 border border-gray-700 rounded-lg pl-4 pr-12 py-4 focus:outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500 transition-all shadow-inner text-gray-100"
            />
            <button className="absolute right-3 top-3.5 p-1.5 bg-blue-600 hover:bg-blue-500 rounded text-white transition-colors">
              <Code size={18} />
            </button>
          </div>
          <div className="max-w-4xl mx-auto mt-2 text-xs text-gray-500 flex gap-4">
            <span>Model: Default (Auto-Routed)</span>
            <span>Evidence Engine: STRICT</span>
          </div>
        </div>
      </div>
    </div>
  );
}
