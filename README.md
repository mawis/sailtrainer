sail trainer
============

These are Android apps for the preparation to different driving licences for
German pleasure crafts on water ways. Essentially these are six copies of the
same source code, that is able to train the correct responses to multiple
choice questions. The user gets asked until he is able to correctly answer the
question five consecutive times.

The SKS trainer is a bit different as the exam does require the examinee to
answer questions in free text.

This is the source code of the Android application. You can install the
applications in compiled form directly from the Google Play Store:

https://play.google.com/store/apps/details?id=eu.wimmerinformatik.sbfb
https://play.google.com/store/apps/details?id=eu.wimmerinformatik.sbfs
https://play.google.com/store/apps/details?id=eu.wimmerinformatik.sks
https://play.google.com/store/apps/details?id=eu.wimmerinformatik.src
https://play.google.com/store/apps/details?id=eu.wimmerinformatik.trainer
https://play.google.com/store/apps/details?id=eu.wimmerinformatik.lrc


Source code repositories
------------------------

All of the source code was in this originally in this repository. When building
continuous integration and deployment for all of the trainers, I moved them to
individual repositories, so that commits trigger the correct pipelines.

The code can be found on:

* https://gitlab.com/sailtrainer/sbfs-trainer
* https://gitlab.com/sailtrainer/sbfb-trainer
* https://gitlab.com/sailtrainer/sks-trainer
* https://gitlab.com/sailtrainer/src-trainer
* https://gitlab.com/sailtrainer/lrc-trainer
* https://gitlab.com/sailtrainer/ubi-trainer

Matthias Wimmer, 2020-01-11
