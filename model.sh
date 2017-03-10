#!/bin/sh
java -ea -cp 'target/brainiac-1.0-SNAPSHOT.jar:target/dependency/*' ru.lj.alamar.brainiac.Model $@
