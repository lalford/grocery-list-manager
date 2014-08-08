$(document).ready(function(){
  $("table td a.delete").click(function () {
    $(this).parent().parent().remove();
  });

  $("table td input.recipe-ingredient").each(function(i) {
    $(this).autocomplete({
      source: "/recipes/autocomplete",
      minLength: 2
    });
  });

  $("table td input.food-ingredient-name").each(function(i) {
    $(this).autocomplete({
      source: "/foods/autocomplete",
      minLength: 2
    });
  });

  $("table td input.food-ingredient-unit").each(function(i) {
    $(this).autocomplete({
      source: "/units/autocomplete",
      minLength: 2
    });
  });

  $("table td input.food-ingredient-store-section").each(function(i) {
    $(this).autocomplete({
      source: "/storeSections/autocomplete",
      minLength: 2
    });
  });

  $("#add-recipe-ingredient-link").click(function() {
    var newIndex = parseInt($("#next-recipe-ingredient-index").text());
    var nextIndex = newIndex + 1;
    $('#next-recipe-ingredient-index').text(nextIndex);

    var nameInput = $('<input>', {
      type: 'text',
      name: 'recipeIngredients[' + newIndex + '].name',
      value: ''
    });

    nameInput.autocomplete({
      source: "/recipes/autocomplete",
      minLength: 2
    });

    var servingsInput = $('<input>', {
      type: 'text',
      name: 'recipeIngredients[' + newIndex + '].desiredServings',
      value: '0.0'
    });

    var removeLink = $('<a>', {
      text: 'Remove',
      click: function(){ $(this).parent().parent().remove(); }
    });

    var nameTd = $('<td>', { class: 'ingredient-table' }).append(nameInput);
    var servingsTd = $('<td>', { class: 'ingredient-table' }).append(servingsInput);
    var removeTd = $('<td>', { class: 'ingredient-table' }).append(removeLink);

    var newTr = $('<tr>').append(nameTd).append(servingsTd).append(removeTd);

    $('#recipe-ingredient-table').find('tr:last').after(newTr);
  });

  $("#add-food-ingredient-link").click(function() {
    var newIndex = parseInt($("#next-food-ingredient-index").text());
    var nextIndex = newIndex + 1;
    $('#next-food-ingredient-index').text(nextIndex);

    var foodInput = $('<input>', {
      type: 'text',
      name: 'foodIngredients[' + newIndex + '].food',
      value: ''
    });

    foodInput.autocomplete({
      source: "/foods/autocomplete",
      minLength: 2
    });

    var quantityInput = $('<input>', {
      type: 'text',
      name: 'foodIngredients[' + newIndex + '].quantity',
      value: ''
    });

    var unitInput = $('<input>', {
      type: 'text',
      name: 'foodIngredients[' + newIndex + '].unit',
      value: ''
    });

    unitInput.autocomplete({
      source: "/units/autocomplete",
      minLength: 2
    });

    var storeSectionInput = $('<input>', {
      type: 'text',
      name: 'foodIngredients[' + newIndex + '].storeSection',
      value: ''
    });

    storeSectionInput.autocomplete({
      source: "/storeSections/autocomplete",
      minLength: 2
    });

    var removeLink = $('<a>', {
      text: 'Remove',
      click: function(){ $(this).parent().parent().remove(); }
    });

    var foodTd = $('<td>', { class: 'ingredient-table' }).append(foodInput);
    var quantityTd = $('<td>', { class: 'ingredient-table' }).append(quantityInput);
    var unitTd = $('<td>', { class: 'ingredient-table' }).append(unitInput);
    var storeSectionTd = $('<td>', { class: 'ingredient-table' }).append(storeSectionInput);
    var removeTd = $('<td>', { class: 'ingredient-table' }).append(removeLink);

    var newTr = $('<tr>').append(foodTd).append(quantityTd).append(unitTd).append(storeSectionTd).append(removeTd);

    $('#food-ingredient-table').find('tr:last').after(newTr);
  });

  $("#add-tag-link").click(function () {
    var newIndex = parseInt($("#next-tag-index").text());
    var nextIndex = newIndex + 1;
    $('#next-tag-index').text(nextIndex);

    var tagInput = $('<input>', {
      type: 'text',
      name: 'tags[' + newIndex + ']',
      value: ''
    });

    var removeLink = $('<a>',{
      text: 'Remove',
      click: function(){ $(this).parent().parent().remove(); }
    });

    var tagTd = $('<td>', { class: 'ingredient-table' }).append(tagInput);
    var removeTd = $('<td>', { class: 'ingredient-table' }).append(removeLink);

    var newTr = $('<tr>').append(tagTd).append(removeTd);

    $('#tag-table').find('tr:last').after(newTr);
  });

  $('.delete-confirmation').on('click', function () {
    return confirm("Really delete?");
  });
});