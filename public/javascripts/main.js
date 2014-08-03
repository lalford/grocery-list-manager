$(document).ready(function(){
  $("table td a.delete").click(function () {
    $(this).parent().parent().remove();
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

    var storeSectionInput = $('<input>', {
      type: 'text',
      name: 'foodIngredients[' + newIndex + '].storeSection',
      value: ''
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

});