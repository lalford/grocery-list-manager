$(document).ready(function(){

  $("#add-recipe-ingredient-link").click(function() {
    var newIndex = parseInt($("#next-recipe-ingredient-index").text());
    var nextIndex = newIndex + 1;

    var nameInput = '<input type="text" name="recipeIngredients[' + newIndex + '].name" value="">';
    var servingsInput = '<input type="text" name="recipeIngredients[' + newIndex + '].desiredServings" value="0.0">';

    var newRow = '<tr><td class="ingredient-table">' + nameInput + '</td><td class="ingredient-table">' + servingsInput + '</td></tr>';

    $('#next-recipe-ingredient-index').text(nextIndex);
    $('#recipe-ingredient-table tr:last').after(newRow);
  });

  $("#add-food-ingredient-link").click(function() {
    var newIndex = parseInt($("#next-food-ingredient-index").text());
    var nextIndex = newIndex + 1;
    $('#next-food-ingredient-index').text(nextIndex);

    var foodInput = '<input type="text" name="foodIngredients[' + newIndex + '].food" value="">';
    var quantityInput = '<input type="text" name="foodIngredients[' + newIndex + '].quantity" value="">';
    var unitInput = '<input type="text" name="foodIngredients[' + newIndex + '].unit" value="">';
    var storeSectionInput = '<input type="text" name="foodIngredients[' + newIndex + '].storeSection" value="">';

    var newRow = '<tr><td class="ingredient-table">' + foodInput + '</td><td class="ingredient-table">' + quantityInput + '</td><td class="ingredient-table">' + unitInput + '</td><td class="ingredient-table">' + storeSectionInput + '</td></tr>';
    $('#food-ingredient-table tr:last').after(newRow);
  });

});