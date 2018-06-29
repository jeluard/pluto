// Compiled by ClojureScript 1.10.329 {:static-fns true, :optimize-constants true}
goog.provide('clojure.data');
goog.require('cljs.core');
goog.require('cljs.core.constants');
goog.require('clojure.set');
/**
 * Internal helper for diff.
 */
clojure.data.atom_diff = (function clojure$data$atom_diff(a,b){
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(a,b)){
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [null,null,a], null);
} else {
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [a,b,null], null);
}
});
/**
 * Convert an associative-by-numeric-index collection into
 * an equivalent vector, with nil for any missing keys
 */
clojure.data.vectorize = (function clojure$data$vectorize(m){
if(cljs.core.seq(m)){
return cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (result,p__5045){
var vec__5046 = p__5045;
var k = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__5046,(0),null);
var v = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__5046,(1),null);
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(result,k,v);
}),cljs.core.vec(cljs.core.repeat.cljs$core$IFn$_invoke$arity$2(cljs.core.apply.cljs$core$IFn$_invoke$arity$2(cljs.core.max,cljs.core.keys(m)),null)),m);
} else {
return null;
}
});
/**
 * Diff associative things a and b, comparing only the key k.
 */
clojure.data.diff_associative_key = (function clojure$data$diff_associative_key(a,b,k){
var va = cljs.core.get.cljs$core$IFn$_invoke$arity$2(a,k);
var vb = cljs.core.get.cljs$core$IFn$_invoke$arity$2(b,k);
var vec__5049 = clojure.data.diff(va,vb);
var a_STAR_ = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__5049,(0),null);
var b_STAR_ = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__5049,(1),null);
var ab = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__5049,(2),null);
var in_a = cljs.core.contains_QMARK_(a,k);
var in_b = cljs.core.contains_QMARK_(b,k);
var same = ((in_a) && (in_b) && (((!((ab == null))) || ((((va == null)) && ((vb == null)))))));
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [((((in_a) && (((!((a_STAR_ == null))) || (!(same))))))?cljs.core.PersistentArrayMap.createAsIfByAssoc([k,a_STAR_]):null),((((in_b) && (((!((b_STAR_ == null))) || (!(same))))))?cljs.core.PersistentArrayMap.createAsIfByAssoc([k,b_STAR_]):null),((same)?cljs.core.PersistentArrayMap.createAsIfByAssoc([k,ab]):null)], null);
});
/**
 * Diff associative things a and b, comparing only keys in ks (if supplied).
 */
clojure.data.diff_associative = (function clojure$data$diff_associative(var_args){
var G__5053 = arguments.length;
switch (G__5053) {
case 2:
return clojure.data.diff_associative.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return clojure.data.diff_associative.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",cljs.core.str.cljs$core$IFn$_invoke$arity$1(arguments.length)].join('')));

}
});

clojure.data.diff_associative.cljs$core$IFn$_invoke$arity$2 = (function (a,b){
return clojure.data.diff_associative.cljs$core$IFn$_invoke$arity$3(a,b,clojure.set.union.cljs$core$IFn$_invoke$arity$2(cljs.core.keys(a),cljs.core.keys(b)));
});

clojure.data.diff_associative.cljs$core$IFn$_invoke$arity$3 = (function (a,b,ks){
return cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (diff1,diff2){
return cljs.core.doall.cljs$core$IFn$_invoke$arity$1(cljs.core.map.cljs$core$IFn$_invoke$arity$3(cljs.core.merge,diff1,diff2));
}),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [null,null,null], null),cljs.core.map.cljs$core$IFn$_invoke$arity$2(cljs.core.partial.cljs$core$IFn$_invoke$arity$3(clojure.data.diff_associative_key,a,b),ks));
});

clojure.data.diff_associative.cljs$lang$maxFixedArity = 3;

clojure.data.diff_sequential = (function clojure$data$diff_sequential(a,b){
return cljs.core.vec(cljs.core.map.cljs$core$IFn$_invoke$arity$2(clojure.data.vectorize,clojure.data.diff_associative.cljs$core$IFn$_invoke$arity$3(((cljs.core.vector_QMARK_(a))?a:cljs.core.vec(a)),((cljs.core.vector_QMARK_(b))?b:cljs.core.vec(b)),cljs.core.range.cljs$core$IFn$_invoke$arity$1((function (){var x__4024__auto__ = cljs.core.count(a);
var y__4025__auto__ = cljs.core.count(b);
return ((x__4024__auto__ > y__4025__auto__) ? x__4024__auto__ : y__4025__auto__);
})()))));
});
clojure.data.diff_set = (function clojure$data$diff_set(a,b){
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [cljs.core.not_empty(clojure.set.difference.cljs$core$IFn$_invoke$arity$2(a,b)),cljs.core.not_empty(clojure.set.difference.cljs$core$IFn$_invoke$arity$2(b,a)),cljs.core.not_empty(clojure.set.intersection.cljs$core$IFn$_invoke$arity$2(a,b))], null);
});

/**
 * Implementation detail. Subject to change.
 * @interface
 */
clojure.data.EqualityPartition = function(){};

/**
 * Implementation detail. Subject to change.
 */
clojure.data.equality_partition = (function clojure$data$equality_partition(x){
if(((!((x == null))) && (!((x.clojure$data$EqualityPartition$equality_partition$arity$1 == null))))){
return x.clojure$data$EqualityPartition$equality_partition$arity$1(x);
} else {
var x__4230__auto__ = (((x == null))?null:x);
var m__4231__auto__ = (clojure.data.equality_partition[goog.typeOf(x__4230__auto__)]);
if(!((m__4231__auto__ == null))){
return (m__4231__auto__.cljs$core$IFn$_invoke$arity$1 ? m__4231__auto__.cljs$core$IFn$_invoke$arity$1(x) : m__4231__auto__.call(null,x));
} else {
var m__4231__auto____$1 = (clojure.data.equality_partition["_"]);
if(!((m__4231__auto____$1 == null))){
return (m__4231__auto____$1.cljs$core$IFn$_invoke$arity$1 ? m__4231__auto____$1.cljs$core$IFn$_invoke$arity$1(x) : m__4231__auto____$1.call(null,x));
} else {
throw cljs.core.missing_protocol("EqualityPartition.equality-partition",x);
}
}
}
});


/**
 * Implementation detail. Subject to change.
 * @interface
 */
clojure.data.Diff = function(){};

/**
 * Implementation detail. Subject to change.
 */
clojure.data.diff_similar = (function clojure$data$diff_similar(a,b){
if(((!((a == null))) && (!((a.clojure$data$Diff$diff_similar$arity$2 == null))))){
return a.clojure$data$Diff$diff_similar$arity$2(a,b);
} else {
var x__4230__auto__ = (((a == null))?null:a);
var m__4231__auto__ = (clojure.data.diff_similar[goog.typeOf(x__4230__auto__)]);
if(!((m__4231__auto__ == null))){
return (m__4231__auto__.cljs$core$IFn$_invoke$arity$2 ? m__4231__auto__.cljs$core$IFn$_invoke$arity$2(a,b) : m__4231__auto__.call(null,a,b));
} else {
var m__4231__auto____$1 = (clojure.data.diff_similar["_"]);
if(!((m__4231__auto____$1 == null))){
return (m__4231__auto____$1.cljs$core$IFn$_invoke$arity$2 ? m__4231__auto____$1.cljs$core$IFn$_invoke$arity$2(a,b) : m__4231__auto____$1.call(null,a,b));
} else {
throw cljs.core.missing_protocol("Diff.diff-similar",a);
}
}
}
});

goog.object.set(clojure.data.EqualityPartition,"null",true);

var G__5055_5079 = clojure.data.equality_partition;
var G__5056_5080 = "null";
var G__5057_5081 = ((function (G__5055_5079,G__5056_5080){
return (function (x){
return cljs.core.cst$kw$atom;
});})(G__5055_5079,G__5056_5080))
;
goog.object.set(G__5055_5079,G__5056_5080,G__5057_5081);

goog.object.set(clojure.data.EqualityPartition,"string",true);

var G__5058_5082 = clojure.data.equality_partition;
var G__5059_5083 = "string";
var G__5060_5084 = ((function (G__5058_5082,G__5059_5083){
return (function (x){
return cljs.core.cst$kw$atom;
});})(G__5058_5082,G__5059_5083))
;
goog.object.set(G__5058_5082,G__5059_5083,G__5060_5084);

goog.object.set(clojure.data.EqualityPartition,"number",true);

var G__5061_5085 = clojure.data.equality_partition;
var G__5062_5086 = "number";
var G__5063_5087 = ((function (G__5061_5085,G__5062_5086){
return (function (x){
return cljs.core.cst$kw$atom;
});})(G__5061_5085,G__5062_5086))
;
goog.object.set(G__5061_5085,G__5062_5086,G__5063_5087);

goog.object.set(clojure.data.EqualityPartition,"array",true);

var G__5064_5088 = clojure.data.equality_partition;
var G__5065_5089 = "array";
var G__5066_5090 = ((function (G__5064_5088,G__5065_5089){
return (function (x){
return cljs.core.cst$kw$sequential;
});})(G__5064_5088,G__5065_5089))
;
goog.object.set(G__5064_5088,G__5065_5089,G__5066_5090);

goog.object.set(clojure.data.EqualityPartition,"function",true);

var G__5067_5091 = clojure.data.equality_partition;
var G__5068_5092 = "function";
var G__5069_5093 = ((function (G__5067_5091,G__5068_5092){
return (function (x){
return cljs.core.cst$kw$atom;
});})(G__5067_5091,G__5068_5092))
;
goog.object.set(G__5067_5091,G__5068_5092,G__5069_5093);

goog.object.set(clojure.data.EqualityPartition,"boolean",true);

var G__5070_5094 = clojure.data.equality_partition;
var G__5071_5095 = "boolean";
var G__5072_5096 = ((function (G__5070_5094,G__5071_5095){
return (function (x){
return cljs.core.cst$kw$atom;
});})(G__5070_5094,G__5071_5095))
;
goog.object.set(G__5070_5094,G__5071_5095,G__5072_5096);

goog.object.set(clojure.data.EqualityPartition,"_",true);

var G__5073_5097 = clojure.data.equality_partition;
var G__5074_5098 = "_";
var G__5075_5099 = ((function (G__5073_5097,G__5074_5098){
return (function (x){
if(((!((x == null)))?(((((x.cljs$lang$protocol_mask$partition0$ & (1024))) || ((cljs.core.PROTOCOL_SENTINEL === x.cljs$core$IMap$))))?true:(((!x.cljs$lang$protocol_mask$partition0$))?cljs.core.native_satisfies_QMARK_(cljs.core.IMap,x):false)):cljs.core.native_satisfies_QMARK_(cljs.core.IMap,x))){
return cljs.core.cst$kw$map;
} else {
if(((!((x == null)))?(((((x.cljs$lang$protocol_mask$partition0$ & (4096))) || ((cljs.core.PROTOCOL_SENTINEL === x.cljs$core$ISet$))))?true:(((!x.cljs$lang$protocol_mask$partition0$))?cljs.core.native_satisfies_QMARK_(cljs.core.ISet,x):false)):cljs.core.native_satisfies_QMARK_(cljs.core.ISet,x))){
return cljs.core.cst$kw$set;
} else {
if(((!((x == null)))?(((((x.cljs$lang$protocol_mask$partition0$ & (16777216))) || ((cljs.core.PROTOCOL_SENTINEL === x.cljs$core$ISequential$))))?true:(((!x.cljs$lang$protocol_mask$partition0$))?cljs.core.native_satisfies_QMARK_(cljs.core.ISequential,x):false)):cljs.core.native_satisfies_QMARK_(cljs.core.ISequential,x))){
return cljs.core.cst$kw$sequential;
} else {
return cljs.core.cst$kw$atom;

}
}
}
});})(G__5073_5097,G__5074_5098))
;
goog.object.set(G__5073_5097,G__5074_5098,G__5075_5099);
goog.object.set(clojure.data.Diff,"null",true);

var G__5100_5124 = clojure.data.diff_similar;
var G__5101_5125 = "null";
var G__5102_5126 = ((function (G__5100_5124,G__5101_5125){
return (function (a,b){
return clojure.data.atom_diff(a,b);
});})(G__5100_5124,G__5101_5125))
;
goog.object.set(G__5100_5124,G__5101_5125,G__5102_5126);

goog.object.set(clojure.data.Diff,"string",true);

var G__5103_5127 = clojure.data.diff_similar;
var G__5104_5128 = "string";
var G__5105_5129 = ((function (G__5103_5127,G__5104_5128){
return (function (a,b){
return clojure.data.atom_diff(a,b);
});})(G__5103_5127,G__5104_5128))
;
goog.object.set(G__5103_5127,G__5104_5128,G__5105_5129);

goog.object.set(clojure.data.Diff,"number",true);

var G__5106_5130 = clojure.data.diff_similar;
var G__5107_5131 = "number";
var G__5108_5132 = ((function (G__5106_5130,G__5107_5131){
return (function (a,b){
return clojure.data.atom_diff(a,b);
});})(G__5106_5130,G__5107_5131))
;
goog.object.set(G__5106_5130,G__5107_5131,G__5108_5132);

goog.object.set(clojure.data.Diff,"array",true);

var G__5109_5133 = clojure.data.diff_similar;
var G__5110_5134 = "array";
var G__5111_5135 = ((function (G__5109_5133,G__5110_5134){
return (function (a,b){
return clojure.data.diff_sequential(a,b);
});})(G__5109_5133,G__5110_5134))
;
goog.object.set(G__5109_5133,G__5110_5134,G__5111_5135);

goog.object.set(clojure.data.Diff,"function",true);

var G__5112_5136 = clojure.data.diff_similar;
var G__5113_5137 = "function";
var G__5114_5138 = ((function (G__5112_5136,G__5113_5137){
return (function (a,b){
return clojure.data.atom_diff(a,b);
});})(G__5112_5136,G__5113_5137))
;
goog.object.set(G__5112_5136,G__5113_5137,G__5114_5138);

goog.object.set(clojure.data.Diff,"boolean",true);

var G__5115_5139 = clojure.data.diff_similar;
var G__5116_5140 = "boolean";
var G__5117_5141 = ((function (G__5115_5139,G__5116_5140){
return (function (a,b){
return clojure.data.atom_diff(a,b);
});})(G__5115_5139,G__5116_5140))
;
goog.object.set(G__5115_5139,G__5116_5140,G__5117_5141);

goog.object.set(clojure.data.Diff,"_",true);

var G__5118_5142 = clojure.data.diff_similar;
var G__5119_5143 = "_";
var G__5120_5144 = ((function (G__5118_5142,G__5119_5143){
return (function (a,b){
var fexpr__5122 = (function (){var G__5123 = clojure.data.equality_partition(a);
var G__5123__$1 = (((G__5123 instanceof cljs.core.Keyword))?G__5123.fqn:null);
switch (G__5123__$1) {
case "atom":
return clojure.data.atom_diff;

break;
case "set":
return clojure.data.diff_set;

break;
case "sequential":
return clojure.data.diff_sequential;

break;
case "map":
return clojure.data.diff_associative;

break;
default:
throw (new Error(["No matching clause: ",cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__5123__$1)].join('')));

}
})();
return (fexpr__5122.cljs$core$IFn$_invoke$arity$2 ? fexpr__5122.cljs$core$IFn$_invoke$arity$2(a,b) : fexpr__5122.call(null,a,b));
});})(G__5118_5142,G__5119_5143))
;
goog.object.set(G__5118_5142,G__5119_5143,G__5120_5144);
/**
 * Recursively compares a and b, returning a tuple of
 *   [things-only-in-a things-only-in-b things-in-both].
 *   Comparison rules:
 * 
 *   * For equal a and b, return [nil nil a].
 *   * Maps are subdiffed where keys match and values differ.
 *   * Sets are never subdiffed.
 *   * All sequential things are treated as associative collections
 *  by their indexes, with results returned as vectors.
 *   * Everything else (including strings!) is treated as
 *  an atom and compared for equality.
 */
clojure.data.diff = (function clojure$data$diff(a,b){
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(a,b)){
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [null,null,a], null);
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(clojure.data.equality_partition(a),clojure.data.equality_partition(b))){
return clojure.data.diff_similar(a,b);
} else {
return clojure.data.atom_diff(a,b);
}
}
});