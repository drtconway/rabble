
rangy.init();

var CiterButton = MediumEditor.extensions.button.extend({
    name: 'citer',
    tagNames: ['cite'],
    contentDefault: '<b>[]</b>',
    contentFA: '<i class="fa fa-book"></i>',
    aria: 'Cite',
    action: 'cite',

    init: function () {
        MediumEditor.extensions.button.prototype.init.call(this);

        this.classApplier = rangy.createClassApplier('cite', {
            elementTagName: 'cite',
            normalize: true
        });
    },

    handleClick: function (event) {
        this.classApplier.toggleSelection();

        // Ensure the editor knows about an html change so watchers are notified
        // ie: <textarea> elements depend on the editableInput event to stay synchronized
        this.base.checkContentChanged();
    }
});

document.querySelectorAll('[data-rabble-name][data-rabble-edit="true"]').forEach((e) => {
    // Fill in the default "text" kind.
    if (!e.hasAttribute('data-rabble-kind')) {
        e.setAttribute('data-rabble-kind', 'text');
    }
});

document.querySelectorAll('[data-rabble-kind="rich"][data-rabble-edit="true"]').forEach((e) => {
    e.setAttribute('contenteditable', 'true');
    e.classList.add("editable");
    new MediumEditor(e, {
        toolbar: {
            static: true,
            sticky: true,
            buttons: ['bold', 'italic', 'underline']
        }
    });
});
document.querySelectorAll('[data-rabble-kind="lines"][data-rabble-edit="true"]').forEach((e) => {
    e.setAttribute('contenteditable', 'true');
    e.classList.add("editable");
    new MediumEditor(e, {
        toolbar: {
            buttons: []
        }
    });
});
document.querySelectorAll('[data-rabble-kind="text"][data-rabble-edit="true"').forEach((e) => {
    e.setAttribute('contenteditable', 'true');
    e.classList.add("editable");
    new MediumEditor(e, {
      disableReturn: true,
      placeholder: {
        text: 'type here'
      }
    });
});
