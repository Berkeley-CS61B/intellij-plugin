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
		"trace":[{
			"stdout":"",
			"event":"step_line",
			"line":40,
			"stack_to_render":[{
				"func_name":"main:40",
				"encoded_locals":{"mc":["REF",145]},
				"ordered_varnames":["mc"],
				"parent_frame_id_list":[],
				"is_highlighted":true,
				"is_zombie":false,
				"is_parent":false,
				"unique_hash":"195",
				"frame_id":195
			}],
			"globals":{},
			"ordered_globals":[],
			"func_name":"main",
			"heap":{
				"145":["INSTANCE","LinkedList",["first",["REF",172]]],
				"172":["INSTANCE","Node",["next",["REF",174]],["name","DK Sr."]],
				"174":["INSTANCE","Node",["next",["REF",176]],["name","DK"]],
				"176":["INSTANCE","Node",["next",null],["name","DK Jr."]]}
		}],
		"userlog":""
	};
	var v = new ExecutionVisualizer('root', trace, opts);
});