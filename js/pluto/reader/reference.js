// Compiled by ClojureScript 1.10.439 {:static-fns true, :optimize-constants true}
goog.provide('pluto.reader.reference');
goog.require('cljs.core');
goog.require('cljs.core.constants');
goog.require('pluto.reader.errors');
/**
 * Return true if argument is a reference
 */
pluto.reader.reference.reference_QMARK_ = (function pluto$reader$reference$reference_QMARK_(o){
var and__4036__auto__ = cljs.core.vector_QMARK_(o);
if(and__4036__auto__){
var and__4036__auto____$1 = (function (){var c = cljs.core.count(o);
return ((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2((1),c)) || (cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2((2),c)));
})();
if(and__4036__auto____$1){
return (cljs.core.first(o) instanceof cljs.core.Symbol);
} else {
return and__4036__auto____$1;
}
} else {
return and__4036__auto__;
}
});
/**
 * Return the symbol pointed by the reference
 * 
 * ```clojure
 * (= 'some.ref (reference->symbol ['some.ref]))
 * ```
 */
pluto.reader.reference.reference__GT_symbol = (function pluto$reader$reference$reference__GT_symbol(o){
if(pluto.reader.reference.reference_QMARK_(o)){
return cljs.core.first(o);
} else {
return null;
}
});
pluto.reader.reference.type__GT_ns = new cljs.core.PersistentArrayMap(null, 3, [cljs.core.cst$kw$view,"views",cljs.core.cst$kw$query,"queries",cljs.core.cst$kw$event,"events"], null);
pluto.reader.reference.type__GT_capacity = new cljs.core.PersistentArrayMap(null, 3, [cljs.core.cst$kw$view,cljs.core.cst$kw$components,cljs.core.cst$kw$query,cljs.core.cst$kw$queries,cljs.core.cst$kw$event,cljs.core.cst$kw$events], null);
/**
 * Resolve a symbol first via the extension definition then via the host ctx.
 */
pluto.reader.reference.resolve_symbol = (function pluto$reader$reference$resolve_symbol(ctx,ext,type,ns,s){
var or__4047__auto__ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(ext,cljs.core.symbol.cljs$core$IFn$_invoke$arity$2(ns,cljs.core.name(s)));
if(cljs.core.truth_(or__4047__auto__)){
return or__4047__auto__;
} else {
return cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(ctx,new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [cljs.core.cst$kw$capacities,cljs.core.get.cljs$core$IFn$_invoke$arity$2(pluto.reader.reference.type__GT_capacity,type),s,cljs.core.cst$kw$value], null));
}
});
pluto.reader.reference.valid_reference_QMARK_ = (function pluto$reader$reference$valid_reference_QMARK_(ref){
if(cljs.core.vector_QMARK_(ref)){
var vec__1173 = ref;
var name = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__1173,(0),null);
var arguments$ = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__1173,(1),null);
return (((name instanceof cljs.core.Symbol)) && (((2) >= cljs.core.count(ref))) && ((((arguments$ == null)) || (cljs.core.map_QMARK_(arguments$)) || ((arguments$ instanceof cljs.core.Symbol)))));
} else {
return null;
}
});
/**
 * Resolve a reference defined by a hook
 * 
 * ```clojure
 * (= {:data "view"} (resolve {} {'views/id "view"} :view ['id]))
 * ```
 */
pluto.reader.reference.resolve = (function pluto$reader$reference$resolve(ctx,ext,type,value){
if(cljs.core.truth_(pluto.reader.reference.valid_reference_QMARK_(value))){
var s = pluto.reader.reference.reference__GT_symbol(value);
var temp__5718__auto__ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(pluto.reader.reference.type__GT_ns,type);
if(cljs.core.truth_(temp__5718__auto__)){
var ns = temp__5718__auto__;
var temp__5718__auto____$1 = pluto.reader.reference.resolve_symbol(ctx,ext,type,ns,s);
if(cljs.core.truth_(temp__5718__auto____$1)){
var o = temp__5718__auto____$1;
return new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$data,o], null);
} else {
return new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$errors,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [pluto.reader.errors.error.cljs$core$IFn$_invoke$arity$2(cljs.core.cst$kw$pluto$reader$errors_SLASH_unknown_DASH_reference,new cljs.core.PersistentArrayMap(null, 2, [cljs.core.cst$kw$value,s,cljs.core.cst$kw$type,type], null))], null)], null);
}
} else {
return new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$errors,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [pluto.reader.errors.error.cljs$core$IFn$_invoke$arity$2(cljs.core.cst$kw$pluto$reader$errors_SLASH_unknown_DASH_reference_DASH_type,new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$value,type], null))], null)], null);
}
} else {
return new cljs.core.PersistentArrayMap(null, 1, [cljs.core.cst$kw$errors,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [pluto.reader.errors.error.cljs$core$IFn$_invoke$arity$2(cljs.core.cst$kw$pluto$reader$errors_SLASH_invalid_DASH_reference,new cljs.core.PersistentArrayMap(null, 2, [cljs.core.cst$kw$type,type,cljs.core.cst$kw$value,value], null))], null)], null);
}
});
