$(document).ready(function(){
    $( "#accordion" ).accordion();
    $( ".selector" ).accordion({ heightStyle: "content" });

//    var recipes = [];
//
//    $.getJSON("/recipes", function(results){
//        $.each(results, function(i, field){
//            recipes.push(field);
//            console.log(i + ") " + field["name"]);
//        });
//    });
});