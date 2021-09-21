/**
 * The sqldatabse package contains classes and interfaces to access data in a sql database through
 * writing prepared statements. Instances of {@link se.uu.ub.cora.sqldatabase.DatabaseFacade} can be
 * created using {@link se.uu.ub.cora.sqldatabase.SqlDatabaseFactoryImp#factorDatabaseFacade()}.
 * <p>
 * If a higher level access to tables is sufficient have a look at the
 * {@link se.uu.ub.cora.sqldatabase.table} package. Instances of
 * {@link se.uu.ub.cora.sqldatabase.table.TableFacade} can be created using
 * {@link SqlDatabaseFactoryImp#factorTableFacade()}.
 */
package se.uu.ub.cora.sqldatabase;