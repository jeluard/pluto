// Compiled by ClojureScript 1.10.516 {:static-fns true, :optimize-constants true}
goog.provide('pluto.utils');
goog.require('cljs.core');
goog.require('cljs.core.constants');
goog.require('clojure.set');
goog.require('clojure.string');
goog.require('pluto.reader.errors');
goog.require('goog.string');
goog.require('goog.string.format');
pluto.utils.ex_cause = (function pluto$utils$ex_cause(ex){
return cljs.core.ex_cause(ex);
});
pluto.utils.ex_message = (function pluto$utils$ex_message(ex){
return cljs.core.ex_message(ex);
});
pluto.utils.primitive_QMARK_ = (function pluto$utils$primitive_QMARK_(o){
return ((cljs.core.boolean_QMARK_(o)) || (cljs.core.int_QMARK_(o)) || (cljs.core.float_QMARK_(o)) || (typeof o === 'string'));
});
pluto.utils.placeholder_pattern = /\$\{([^\{]*)\}/;
/**
 * Extract a collection of placeholders from a string.
 * (placeholders  "")
 */
pluto.utils.placeholders = (function pluto$utils$placeholders(s){
return cljs.core.map.cljs$core$IFn$_invoke$arity$2(cljs.core.comp.cljs$core$IFn$_invoke$arity$3((function (p1__5329_SHARP_){
return cljs.core.PersistentHashMap.fromArrays([cljs.core.cst$kw$name,cljs.core.cst$kw$pattern],[cljs.core.symbol.cljs$core$IFn$_invoke$arity$1(cljs.core.first(p1__5329_SHARP_)),cljs.core.second(p1__5329_SHARP_)]);
}),(function (p1__5330_SHARP_){
return clojure.string.split.cljs$core$IFn$_invoke$arity$2(p1__5330_SHARP_,/:/);
}),cljs.core.second),cljs.core.re_seq(pluto.utils.placeholder_pattern,s));
});
/**
 * `format` but using `en` locale
 */
pluto.utils.default_format = (function pluto$utils$default_format(pattern){
return ["%",cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__4131__auto__ = cljs.core.second(clojure.string.split.cljs$core$IFn$_invoke$arity$2(pattern,/:/));
if(cljs.core.truth_(or__4131__auto__)){
return or__4131__auto__;
} else {
return pattern;
}
})())].join('');
});
if((typeof pluto !== 'undefined') && (typeof pluto.utils !== 'undefined') && (typeof pluto.utils.format_pattern !== 'undefined')){
} else {
/**
 * Extract the format pattern from the full format.
 * Dispatch is done using the last character of the format.
 * 
 * e.g. format name:5s is dispatched using 's'
 */
pluto.utils.format_pattern = (function (){var method_table__4613__auto__ = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(cljs.core.PersistentArrayMap.EMPTY);
var prefer_table__4614__auto__ = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(cljs.core.PersistentArrayMap.EMPTY);
var method_cache__4615__auto__ = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(cljs.core.PersistentArrayMap.EMPTY);
var cached_hierarchy__4616__auto__ = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(cljs.core.PersistentArrayMap.EMPTY);
var hierarchy__4617__auto__ = cljs.core.get.cljs$core$IFn$_invoke$arity$3(cljs.core.PersistentArrayMap.EMPTY,cljs.core.cst$kw$hierarchy,(function (){var fexpr__5331 = cljs.core.get_global_hierarchy;
return (fexpr__5331.cljs$core$IFn$_invoke$arity$0 ? fexpr__5331.cljs$core$IFn$_invoke$arity$0() : fexpr__5331.call(null));
})());
return (new cljs.core.MultiFn(cljs.core.symbol.cljs$core$IFn$_invoke$arity$2("pluto.utils","format-pattern"),((function (method_table__4613__auto__,prefer_table__4614__auto__,method_cache__4615__auto__,cached_hierarchy__4616__auto__,hierarchy__4617__auto__){
return (function (pattern){
if(clojure.string.includes_QMARK_(pattern,":")){
return cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.last(pattern));
} else {
return null;
}
});})(method_table__4613__auto__,prefer_table__4614__auto__,method_cache__4615__auto__,cached_hierarchy__4616__auto__,hierarchy__4617__auto__))
,cljs.core.cst$kw$default,hierarchy__4617__auto__,method_table__4613__auto__,prefer_table__4614__auto__,method_cache__4615__auto__,cached_hierarchy__4616__auto__));
})();
}
pluto.utils.format_pattern.cljs$core$IMultiFn$_add_method$arity$3(null,"f",(function (pattern){
return pluto.utils.default_format(pattern);
}));
pluto.utils.format_pattern.cljs$core$IMultiFn$_add_method$arity$3(null,"d",(function (pattern){
return pluto.utils.default_format(pattern);
}));
pluto.utils.format_pattern.cljs$core$IMultiFn$_add_method$arity$3(null,"s",(function (pattern){
return pluto.utils.default_format(pattern);
}));
pluto.utils.format_pattern.cljs$core$IMultiFn$_add_method$arity$3(null,cljs.core.cst$kw$default,(function (_){
return "%s";
}));
/**
 * Interpolates placeholders inside a string.
 * Returns an error if a placeholder can't be resolved.
 */
pluto.utils.interpolate = (function pluto$utils$interpolate(values,s){
var v = pluto.utils.placeholders(s);
var names = cljs.core.map.cljs$core$IFn$_invoke$arity$2(cljs.core.cst$kw$name,v);
var extra = clojure.set.difference.cljs$core$IFn$_invoke$arity$2(cljs.core.set(names),cljs.core.set(cljs.core.keys(values)));
if(cljs.core.seq(extra)){
return new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$errors,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [pluto.reader.errors.error.cljs$core$IFn$_invoke$arity$2(cljs.core.cst$kw$pluto$reader$errors_SLASH_invalid_DASH_placeholders,extra)], null)], null);
} else {
return new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$data,((cljs.core.seq(v))?cljs.core.apply.cljs$core$IFn$_invoke$arity$3(goog.string.format,clojure.string.replace(s,pluto.utils.placeholder_pattern,((function (v,names,extra){
return (function (p__5332){
var vec__5333 = p__5332;
var _ = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__5333,(0),null);
var pattern = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__5333,(1),null);
return (pluto.utils.format_pattern.cljs$core$IFn$_invoke$arity$1 ? pluto.utils.format_pattern.cljs$core$IFn$_invoke$arity$1(pattern) : pluto.utils.format_pattern.call(null,pattern));
});})(v,names,extra))
),cljs.core.map.cljs$core$IFn$_invoke$arity$2(values,names)):s)], null);
}
});
