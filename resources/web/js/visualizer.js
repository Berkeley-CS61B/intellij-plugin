$(document).ready(function () {
	var opts = {
		hideCode: true,
		hideOutput: true,
		disableHeapNesting: true,
		textualMemoryLabels: false,
		visualizerIdOverride: "1",
		startingInstruction: 0,
		lang: "java"
	};
	var trace = {
		"code":"",
		"stdin":"",
		"trace":[{ "stdout":"", "event":"step_line", "line":11, "stack_to_render":[{   "func_name":"main:11",   "encoded_locals":{  "stack":[ "REF", 169  ],  "queue":[ "REF", 172  ]   },   "ordered_varnames":[  "stack",  "queue"   ],   "parent_frame_id_list":[   ],   "is_highlighted":true,   "is_zombie":false,   "is_parent":false,   "unique_hash":"16",   "frame_id":16} ], "globals":{"StackQueue.global":5 }, "ordered_globals":["StackQueue.global" ], "func_name":"main", "heap":{"169":[   "STACK",   "stack-last",   "stack-first"],"172":[   "QUEUE"] }  }],
		"userlog":""
	};
	var v = new ExecutionVisualizer('root', trace, opts);
});