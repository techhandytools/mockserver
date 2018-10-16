var mappings = [];
var mapId = "";
var form = document.getElementById('addForm');
var stubList = document.getElementById('stubs');
var filter = document.getElementById('filter');
var responseBody = document.getElementById('response-body');
var stubModal = $('#editModal');

// load event
window.addEventListener('load', loadAllStubs);

// form submit event
form.addEventListener('submit', addStub);
// Delete event
stubList.addEventListener('click', removeStub);
// Filter event
filter.addEventListener('keyup', filterStubs);


// Enable tool tip
$('#stub').focus(function(e) {
	$('#stub').tooltip('show');
});
$('#response-status').focus(function(e) {
	$('#response-status').tooltip('show');
});
$('#delay-time').focus(function(e) {
	$('#delay-time').tooltip('show');
});
$('#delay-sigma').focus(function(e) {
	$('#delay-sigma').tooltip('show');
});


// Enable json editor
var editor = new JSONEditor(responseBody, {
	mode: 'code',
	statusBar: false
});


// Add stubs
function addStub(e) {
	e.preventDefault();

	// Get input value
	var newStub = document.getElementById('stub');

	// hide tooltip
	$('[data-toggle="tooltip"]').tooltip("hide");

	// Display stub modal
	$('#request-url').val(newStub.value.replace(/\s/g, ""));
	$('#response-status').val('');
	// $('#response-body').val("{}");
	editor.set(JSON.parse("{}"));
	$('#delay-time').val(10);
	$('#delay-sigma').val();
	mapId = uuid();
	if (newStub.value === "" || newStub.value[0] !== '/' || newStub.value[1] === '/' || newStub.value.length === 1) {
		confirm('Please enter valid URL path');
	} else if (newStub.value[0] === '/') {
		stubModal.modal();
		newStub.value = "";
	}

}

//TODO: replace with js uuid generator
function uuid() {
	var seed = Date.now();
	if (window.performance && typeof window.performance.now === "function") {
		seed += performance.now();
	}

	return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
		var r = (seed + Math.random() * 16) % 16 | 0;
		seed = Math.floor(seed / 16);

		return (c === 'x' ? r : r & (0x3 | 0x8)).toString(16);
	});
}


// Remove stub
function removeStub(e) {
	if (e.target.classList.contains('delete')) {
		if (confirm('Delete stub')) {
			var li = e.target.parentElement;
			stubList.removeChild(li);
			removeStubAndSave(li.id);
		}
	}

	if (e.target.classList.contains('edit')) {
		displayStubModal(e.target.parentElement.id)
	}
}

//  Filter stubs
function filterStubs(e) {
	// convert text to lowercase
	var text = e.target.value.toLowerCase();
	// Get stub lists
	var stubs = stubList.getElementsByTagName('li');
	// Convert to an array
	Array.from(stubs).forEach(function(stub) {
		var stubName = stub.firstChild.textContent;
		if (stubName.toLowerCase().indexOf(text) != -1) {
			stub.style.display = 'block';
		} else {
			stub.style.display = 'none';
		}
	});

}

// Fetch all stubs blob
function loadAllStubs() {
	var mapInit = {method: 'GET'};
	var mapReq = new Request('__admin', mapInit);

	fetch(mapReq).then(function(response) {
		if (response.ok) {
			response.json().then(function(json) {
				mappings = json.mappings;
			}).then(refreshStubLists);
		} else {
			console.log('Network request for __admin/mappings failed with response ' + response.status + ': ' + response.statusText);
		}
	});
}

// Edit stub and save in mongo
function editStubAndSave(id, body) {
	var headers = new Headers();
	headers.append('Content-Type', 'application/json');

	var editMapInit = {
		method: 'PUT',
		headers: headers,
		body: JSON.stringify(body)
	};
	var editMapReq = new Request('__admin/mappings/' + id, editMapInit);

	var saveMap = {
		method: 'POST',
		headers: headers
	};
	var saveMapReq = new Request('__admin/mappings/save', saveMap);

	fetch(editMapReq).then(function() {
		loadAllStubs();
	}).catch(function(reason) {
		console.log(reason)
	});
}

// Add stub
function addStubAndSave(body) {
	var headers = new Headers();
	headers.append('Content-Type', 'application/json');

	var addMapInit = {
		method: 'POST',
		body: JSON.stringify(body)
	};
	var addMapReq = new Request('__admin/mappings', addMapInit);

	var saveMap = {
		method: 'POST',
		headers: headers
	};
	var saveMapReq = new Request('__admin/mappings/save', saveMap);

	fetch(addMapReq).then(function(response) {
		if (response.ok) {
			loadAllStubs();
		}
	}).catch(function(e) {
		console.log(e)
	});
}


// Remove stub and save
function removeStubAndSave(id) {
	var removeMapInit = {
		method: 'DELETE'
	};
	var removeMapReq = new Request('__admin/mappings/' + id, removeMapInit);

	fetch(removeMapReq).then(loadAllStubs).catch(function(reason) {
		console.log(reason);
	});
}

// Refresh stub lists
function refreshStubLists() {
	// Empty the stub list
	while (stubList.hasChildNodes()) {
		stubList.removeChild(stubList.firstChild);
	}

	// repopulate the stub list
	mappings.forEach(function(map) {
		// create new li element
		var li = document.createElement('li');
		var listNode = document.createTextNode(map.request.urlPattern);

		li.className = 'list-group-item';
		li.appendChild(listNode);
		li.setAttribute("id", map.id);

		// Create del button element
		var deleteBtn = document.createElement('button');
		deleteBtn.className = 'btn btn-danger btn-sm float-right delete';
		deleteBtn.appendChild(document.createTextNode('Remove'));

		// Create edit button element
		var editBtn = document.createElement('button');
		editBtn.className = 'btn btn-info btn-sm float-right mr-1 edit';
		editBtn.appendChild(document.createTextNode('Edit'));

		// Append button to li
		li.appendChild(deleteBtn);
		// Append edit to li
		li.appendChild(editBtn);

		stubList.appendChild(li);
	});
}

// Save stub
function saveStubModal() {
	var url = $('#request-url').val();
	var responseStatus = $('#response-status').val();
	// var responseBody = $('#response-body').val();
	var responseBody = JSON.stringify(editor.get());
	var responseTime = $('#delay-time').val();
	var responseTimeSigma = $('#delay-sigma').val();

	var stubMap = {};
	var headers = {
		"Content-Type": "application/json"
	};
	var delayDistribution = {
		"type": "lognormal",
		"median": responseTime,
		"sigma": responseTimeSigma
	};

	stubMap.id = mapId;
	stubMap.persistent = true;
	stubMap.request = {};
	stubMap.request.urlPattern = url;
	stubMap.request.method = "ANY";
	stubMap.response = {};
	stubMap.response.status = responseStatus;
	stubMap.response.body = responseBody.toString();
	stubMap.response.headers = headers;
	stubMap.response.delayDistribution = {};
	stubMap.response.delayDistribution = delayDistribution;
	stubMap.uuid = mapId;

	if (searchId(mapId) === false) {
		addStubAndSave(stubMap)
	} else {
		editStubAndSave(mapId, stubMap);
	}
	stubModal.modal("hide");
}

function searchId(id) {
	var studId = [];
	mappings.forEach(function(map) {
		studId.push(map.id)
	});

	return studId.indexOf(id) >= 0;
}

function displayStubModal(id) {
	var displayStub;
	mappings.forEach(function(map) {
		if (map.id === id) {
			displayStub = map;
		}
	});

	$('#request-url').val(displayStub.request.urlPattern);
	$('#response-status').val(displayStub.response.status);
	// $('#response-body').val(displayStub.response.body);
	editor.set(JSON.parse(displayStub.response.body));
	$('#delay-time').val(displayStub.response.delayDistribution.median);
	$('#delay-sigma').val(displayStub.response.delayDistribution.sigma);
	mapId = displayStub.id;
	stubModal.modal();

}


