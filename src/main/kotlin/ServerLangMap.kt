package dev.mee42


val langMapString = """
armasm, arm
actionscript, as
alan, i
angelscript, asc
apache, apacheconf
applescript, osascript
asciidoc, adoc
awk, mawk, nawk, gawk
bash, sh, zsh
brainfuck, bf
csharp, cs
cpp, hpp, cc, hh, c++, h++, cxx, hxx
cos, cls
cmake, cmake.in
capnproto, capnp
clojure, clj
coffeescript, coffee, cson, iced
crmsh, crm, pcmk
crystal, cr
dns, zone, bind
dos, bat, cmd
delphi, dpr, dfm, pas, pascal, freepascal, lazarus, lpr, lfm
diff, patch
django, jinja
dockerfile, docker
dust, dst
erlang, erl
excel, xls, xlsx
extempore, xtlang, xtm
fsharp, fs
fortran, f90, f95
gcode, nc
gams, gms
gauss, gss
godot, gdscript
gn, gni
go, golang
golo, gololang
xml, html, xhtml, rss, atom, xjb, xsd, xsl, plist, svg
http, https
handlebars, hbs, html.hbs, html.handlebars
haskell, hs
haxe, hx
hy, hylang
ini, toml
inform7, i7
java, jsp
javascript, js, jsx
kotlin, kt
lasso, ls, lassoscript
livescript, ls
makefile, mk, mak
markdown, md, mkdown, mkd
mathematica, mma, wl
mirc, mrc
moonscript, moon
nginx, nginxconf
ocaml, ml
objectivec, mm, objc, obj-c
openscad, scad
pf, pf.conf
php, php3, php4, php5, php6, php7
perl, pl, pm
pgsql, postgres, postgresql
powershell, ps, ps1
puppet, pp
python, py, gyp
k, kdb
cshtml, razor, razor-cshtml
reasonml, re
graph, instances
robot, rf
rpm-specfile, rpm, spec, rpm-spec, specfile
ruby, rb, gemspec, podspec, thor, irb
rust, rs
SAS, sas
p21, step, stp
scilab, sci
shell, console
smalltalk, st
solidity, sol
stan, stanfuncs
iecst, scl, stl, structured-text
stylus, styl
supercollider, sc
tcl, tk
terraform, tf, hcl
twig, craftcms
typescript, ts
vbnet, vb
vbscript, vbs
verilog, v
xl, tao
xquery, xpath, xq
yml, yaml
zephir, zep
""".trimIndent()


val langMap = langMapString
    .split('\n')
    .map { line -> line.split(',') }
    .map { names -> names.map { it.trim() } }
    .map { names -> names.first() to names.drop(1) }
    .fold(mutableMapOf<String, String>()) { acc, (key, subs) -> acc.putAll(subs.map { it to key }); acc }