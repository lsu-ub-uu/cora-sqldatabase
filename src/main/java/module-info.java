/**
 * The sqldatabase module provides interfaces and classes to use a sql database in a Cora based
 * system.
 */
module se.uu.ub.cora.sqldatabase {
	requires transitive java.naming;
	requires transitive java.sql;
	requires se.uu.ub.cora.logger;

	exports se.uu.ub.cora.sqldatabase;
	exports se.uu.ub.cora.sqldatabase.table;
}