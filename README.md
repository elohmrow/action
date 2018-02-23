## What is the Point?
This is a small Magnolia module - it's only job at the moment is to override the default save action in the Assets app.
The override is to allow saving of uploaded Assets not only to the DAM (default), but also to an S3 bucket.

I wanted to do this without touching the Assets app - this way you can add this module and get the functionality without
worrying about later patching anything, or when updating, worrying about previously applied patches.

## How do I get it?
```git clone https://github.com/elohmrow/action.git```

## How do I use it?
- Either ```mvn clean install```, then put the resultant ```.jar``` file in some ```WEB-INF/lib```
- or include the source as a dependency in some web app, like so:
```
    <dependency>
      <groupId>atlas.global</groupId>
      <artifactId>action</artifactId>
      <version>1.0-SNAPSHOT</version>
    </dependency>
```

## TODO
Remember: it is simple right now.  Due to the S3 app it relies on, there's a lot of bugs to eventually iron out.
- would be nice to handle zip uploads
- has not been tested on nested folders
- if there are 0 items in the S3 bucket you're uploading to, it won't work.
- would be nice to save also from the Pages app to S3 
- make saving also to S3 _optional_

## Files
```
src
└── main
    ├── java
    │   └── atlas
    │       └── global
    │           ├── CustomSaveDialogAction.java
                    : this is where the real work happens ... explanation below
    │           └── CustomSaveDialogActionDefinition.java
                    : as per standard, this defn only sets an implementationClass --> CustomSaveDialogAction.java
    └── resources
        ├── META-INF
        │   └── magnolia
        │       └── action.xml
                    : module descriptor - in this case, only cites dependencies
        └── action
            └── decorations
                └── dam-app
                    └── apps
                        ├── assets.subApps.detail.actions.commit.yaml
                            : decoration (inside Magnolia module, so no need to set/change properties)
                            : it's job is only to tell the system we are using a different action from the default -->
                              CustomSaveDialogActionDefinition.java
                        └── assets.subApps.detail.editor.form.tabs.asset.fields.yaml
                            : decoration (inside Magnolia module, so no need to set/change properties)
                            : it's job is to re-write the form that we use for uploads in the Assets app
```

## Explanation
... of ```CustomSaveDialogAction.java```

By default, ```SaveDialogAction``` would be called to upload an asset into the DAM
By default, ```info.magnolia.amazon.s3.ui.action.UploadAssetSaveAction``` would be called to upload an asset to S3 

We need to do both.  So we override those two default actions by pointing to our own.

- L58-74 do what ```UploadAssetSaveAction``` normally does
- L78-93 do what ```SaveDialogAction``` normally does
