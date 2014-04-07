$(":button").click(function() {
    var isbn = this.id;
    var stst = '<table class="table table-hover">
                    {{#books}}
    {{/books}}
        </table>';
    alert('About to report lost on ISBN ' + isbn);
    //if(confirm('Confirm?')){
	    $.ajax({
	    	url: "http://localhost:8001/library/v1/books/"+isbn+"?status=lost",
	    	type: "PUT",
	    	contentType: "application/json",
	    });
	    $("button[id="+isbn+"]").attr("disabled", "disabled");
	    $("body").html( renderedPage );
    //}
});