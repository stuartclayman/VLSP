Ajax.Responders.register({
	onCreate: function() {
		//$('busy').show();
	},
	onComplete: function() {
		if (0 == Ajax.activeRequestCount) {
			//$('busy').hide();
		}
	}
});

var canviz;
var current;
var updater;

document.observe('dom:loaded', function() {
	var list;
	
	list = $('graph_scale');
	[4, 2, 1.5, 1, 0.75, 0.5, 0.33, 0.25].each(function(scale) {
		list.options[list.options.length] = new Option(100 * scale + '%', scale, false, 1 == scale);
	});
	
	canviz = new Canviz('graph_container');
	canviz.setScale($F('graph_scale'));

        current=0;

        // start
                     
        //start_updater();
	load_graph();
});


// load graph N
function load_graph(val) {
    var cookie = 0;

    if (val) {
	canviz.load(graph_url(val));
        cookie = get_cookie('xdot');

        if (parseInt(val) > parseInt(cookie)) {
            current = cookie;
        } else {
            current = val;
        }
    } else {
        canviz.load(graph_url());
        cookie = get_cookie('xdot');
        current = cookie;
        start_updater();
    }

    // set reference number in the web page
    $('ref_number').update(current);

}

// determine the graph url, based on arg
function graph_url(val) {
    if (val) {
        stop_updater();
	return 'GRAPH?ref='+val;
    } else {
        return 'GRAPH';
    }
}


function set_graph_scale() {
	canviz.setScale($F('graph_scale'));
	canviz.draw();
}

function change_graph(val) {
    var next = parseInt(current) + parseInt(val);

    // double check
    if (next < 0) {
        next = 0;        
    }

    current = next;

    load_graph(next.toString());
}

function change_scale(inc) {
	var new_scale = $('graph_scale').selectedIndex + inc;
	if (new_scale < 0 || new_scale >= $('graph_scale').options.length) {
		return;
	}
	$('graph_scale').selectedIndex = new_scale;
	set_graph_scale();
}

function view_source() {
	window.open(graph_url(current));
}

function get_cookie ( cookie_name )
{
  var results = document.cookie.match ( '(^|;) ?' + cookie_name + '=([^;]*)(;|$)' );

  if ( results )
    return ( unescape ( results[2] ) );
  else
    return null;
}

function start_updater() {
    if (!updater) {
        updater = new PeriodicalExecuter(function(pe) { load_graph(); }, 2);
    }
}

function stop_updater() {
    if (updater) {
        updater.stop();
        updater = null;
    }
}
