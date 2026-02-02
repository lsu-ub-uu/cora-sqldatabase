/**
 * The sqldatabase module provides interfaces and classes to use a sql database in a Cora based
 * system.
 */
module se.uu.ub.cora.sqldatabase {
	requires transitive java.naming;
	requires java.sql;
	requires se.uu.ub.cora.logger;
	requires org.postgresql.jdbc;

	exports se.uu.ub.cora.sqldatabase;
	exports se.uu.ub.cora.sqldatabase.table;
	exports se.uu.ub.cora.sqldatabase.sequence;
}