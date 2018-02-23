package atlas.global.field;

import info.magnolia.i18nsystem.SimpleTranslator;
import info.magnolia.ui.api.context.UiContext;
import info.magnolia.ui.api.i18n.I18NAuthoringSupport;
import info.magnolia.ui.form.field.definition.SelectFieldOptionDefinition;
import info.magnolia.ui.form.field.factory.SelectFieldFactory;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.vaadin.data.Item;

public class AmazonS3ACLSelectFieldFactory extends SelectFieldFactory<AmazonS3ACLSelectFieldDefinition> {
    private SimpleTranslator i18n;

    @Inject
    public AmazonS3ACLSelectFieldFactory(AmazonS3ACLSelectFieldDefinition definition, Item relatedFieldItem,
                                         UiContext uiContext, I18NAuthoringSupport i18nAuthoringSupport,
                                         SimpleTranslator i18n) {
        super(definition, relatedFieldItem, uiContext, i18nAuthoringSupport);

        this.i18n = i18n;
    }

    @Override
    public List<SelectFieldOptionDefinition> getSelectFieldOptionDefinition() {
        CannedAccessControlList[] acls = CannedAccessControlList.values();
        List<SelectFieldOptionDefinition> selectFieldOptions = new ArrayList<>();

        for (CannedAccessControlList acl : acls) {
            SelectFieldOptionDefinition selectFieldOptionDefinition = new SelectFieldOptionDefinition();

            selectFieldOptionDefinition.setLabel(acl.name());
            selectFieldOptionDefinition.setValue(acl.name());
            selectFieldOptionDefinition.setName(acl.name());
            selectFieldOptions.add(selectFieldOptionDefinition);
        }

        SelectFieldOptionDefinition selectFieldOptionDefinition = new SelectFieldOptionDefinition();

        selectFieldOptionDefinition.setLabel("optional");
        selectFieldOptionDefinition.setValue("optional");
        selectFieldOptionDefinition.setName("optional");
        selectFieldOptionDefinition.setSelected(true);
        selectFieldOptions.add(selectFieldOptionDefinition);

        return selectFieldOptions;
    }
}