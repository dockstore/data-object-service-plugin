[![Build Status](https://travis-ci.org/dockstore/data-object-service-plugin.svg?branch=master)](https://travis-ci.org/dockstore/data-object-service-plugin)
[![Coverage Status](https://coveralls.io/repos/github/dockstore/data-object-service-plugin/badge.svg?branch=develop)](https://coveralls.io/github/dockstore/data-object-service-plugin?branch=develop)

# data-object-service-plugin
[Dockstore Data Object Service](https://github.com/ga4gh/data-object-service-schemas) file preprovisioning plugin

## Usage

The Data Object Service plugin fetches data objects from a provided DOS URI in order to download one of the returned URLs.

For example, if the data object for a given DOS URI references s3 and gcs URIs, the URL for the s3 data object is passed into the s3 plugin for downloading, because a gcs file plugin for Dockstore does not currently exist.

The plugin only supports downloads. Support for uploads will be added later.

```
$ cat test.dos.json
{
  "input_file": {
        "class": "File",
        "path": "dos://ec2-52-26-45-130.us-west-2.compute.amazonaws.com:8080/911bda59-b6f9-4330-9543-c2bf96df1eca"
    },
    "output_file": {
        "class": "File",
        "path": "/tmp/md5sum.txt"
    }
}

$ dockstore tool launch --entry  quay.io/briandoconnor/dockstore-tool-md5sum  --json test.dos.json
Creating directories for run of Dockstore launcher at: ./datastore//launcher-2c670320-9ade-4f9d-9e54-3eff66c29e8d
Provisioning your input files to your local machine
Preparing download location for: #input_file from dos://ec2-52-26-45-130.us-west-2.compute.amazonaws.com:8080/911bda59-b6f9-4330-9543-c2bf96df1eca into directory: ./datastore//launcher-2c670320-9ade-4f9d-9e54-3eff66c29e8d/inputs/6925e57d-ed67-4acf-ae5e-db0d987384c4
Calling on plugin io.dockstore.provision.S3Plugin$S3Provision to provision s3://1000genomes/phase3/data/HG03237/cg_data/ASM_blood/REPORTS/substitutionLengthCoding-GS000017140-ASM.tsv
Calling out to a cwl-runner to run your tool
Executing: cwltool --enable-dev --non-strict --outdir ./datastore//launcher-2c670320-9ade-4f9d-9e54-3eff66c29e8d/outputs/ --tmpdir-prefix ./datastore//launcher-2c670320-9ade-4f9d-9e54-3eff66c29e8d/tmp/ --tmp-outdir-prefix ./datastore//launcher-2c670320-9ade-4f9d-9e54-3eff66c29e8d/working/ ./Dockstore.cwl ./datastore//launcher-2c670320-9ade-4f9d-9e54-3eff66c29e8d/workflow_params.json
/Library/Frameworks/Python.framework/Versions/3.6/bin/cwltool 1.0.20170828135420
...
Provisioning your output files to their final destinations
Registering: #output_file to provision from ./dockstore/dockstore-tool-md5sum-master/datastore/launcher-2c670320-9ade-4f9d-9e54-3eff66c29e8d/outputs/md5sum.txt to : /tmp/md5sum.txt
Provisioning from ./datastore/launcher-2c670320-9ade-4f9d-9e54-3eff66c29e8d/outputs/md5sum.txt to /tmp/md5sum.txt
Downloading: file:///./datastore/launcher-2c670320-9ade-4f9d-9e54-3eff66c29e8d/outputs/md5sum.txt to file:///tmp/md5sum.txt
```

### The Config File

```
[dockstore-file-dos-plugin]
scheme-preference = s3, gs, synapse
```

A basic Dockstore configuration file is available/should be created in `~/.dockstore/config`.
By default, Dockstore attempts to download the first URL whose scheme is supported by one of your locally-installed Dockstore
file provisioning plugins.

You can override the default behavior by configuring the Data-Object-Service plugin with the `scheme-preference` option.
`scheme-preference` lets you specify your preferred order of schemes that the Data Object Service plugin will use to order the
resolved data objects from a provided DOS URI.

Dockstore will attempt to download the first URL from the ordered list whose scheme is supported by one of your
locally-installed Dockstore file provisioning plugins.

Omitting `scheme-preference` from `~/.dockstore/config` returns Dockstore to the default behavior for resolving DOS URIs, as described above.

## Releases

This section describes creating a release of the Data Object Service plugin.

### Prerequisites

[Install](https://datasift.github.io/gitflow/TheHubFlowTools.html) Hubflow. After it is installed, run `git hubflow init` in the
root of your copy of the repo.

### Create the Release

We will be creating a 0.0.4 release as an example.

1. `git hf release start <version number>` -- Creates a new branch based off develop. With `0.0.4` as the version number, the new
branch will be release/0.0.4.
2. `mvn release:prepare` -- Prompts you to:
    1. `What is the release version for "dockstore-file-dos-plugin"?` -- Enter the same version from step 1, e.g., `0.0.4`
    2. `What is SCM release tag or label for "dockstore-file-dos-plugin"` -- Same as previous question, e.g., `0.0.4`
    3. `What is the new development version for "dockstore-file-dos-plugin"?` -- This will default to your previous answer + 1. You will
    probably want to accept the default, which this example would be `0.0.5-SNAPSHOT`.

This will:

1. Do a commit to the pom.xml setting the version to 0.0.4 in the release/0.0.4 branch
2. Tag the head of the release/0.0.4 branch with a `0.0.4` Git tag.
3. Do a maven build
4. Do another commit setting the version in the pom.xml to 0.0.5-SNAPSHOT

Then do `git hf release finish 0.0.4`, which will
* Merge release/0.0.4 into develop
* Merge develop into master
* Delete the release/0.0.4 branch, locally and in the origin
* Push the develop and master branches, as well as the `0.0.4` tag.

#### Create a GitHub release

1. In your browser, go to https://github.com/dockstore/data-object-service-plugin/releases
2. You will see `0.0.4` listed, but it is *not* a GitHub release, it is a only tag. All GitHub releases have Git tags, but not all Git tags
are  GitHub releases, even though the GitHub UI Releases tab doesn't clearly make that distinction. See
 [this issue](https://github.com/bcit-ci/CodeIgniter/issues/3421).
3. Create a GitHub 0.0.4 release
    1. Click `Draft (or Create) a new release`.
    2. Specify the tag, `0.0.4`
    3. Attach the zip file from your local target directory, which will have the version number in it, e.g.,
    dockstore-file-dos-plugin-0.0.4.zip, to the binaries section of the page.
    4. Enter a title and a description.
    5. Click `Publish Release`


