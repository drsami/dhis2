package org.hisp.dhis.sqlview.jdbc;

/*
 * Copyright (c) 2004-2012, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * * Neither the name of the HISP project nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.sqlview.SqlView;
import org.hisp.dhis.sqlview.SqlViewExpandStore;
import org.hisp.dhis.system.util.SqlHelper;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Dang Duy Hieu
 * @version $Id JdbcSqlViewExpandStore.java July 06, 2010$
 */
public class JdbcSqlViewExpandStore
    implements SqlViewExpandStore
{
    private static final Log log = LogFactory.getLog( JdbcSqlViewExpandStore.class );
    
    private static final String PREFIX_CREATEVIEW_QUERY = "CREATE VIEW ";
    private static final String PREFIX_DROPVIEW_QUERY = "DROP VIEW IF EXISTS ";
    private static final String PREFIX_SELECT_QUERY = "SELECT * FROM ";
    private static final String[] types = { "VIEW" };

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private JdbcTemplate jdbcTemplate;

    public void setJdbcTemplate( JdbcTemplate jdbcTemplate )
    {
        this.jdbcTemplate = jdbcTemplate;
    }

    // -------------------------------------------------------------------------
    // Implementing methods
    // -------------------------------------------------------------------------

    @Override
    public Collection<String> getAllSqlViewNames()
    {
        Set<String> viewersName = new HashSet<String>();

        try
        {
            DatabaseMetaData mtdt = jdbcTemplate.getDataSource().getConnection().getMetaData();

            ResultSet rs = mtdt.getTables( null, null, SqlView.PREFIX_VIEWNAME + "%", types );

            while ( rs.next() )
            {
                viewersName.add( rs.getString( "TABLE_NAME" ) );
            }
        }
        catch ( SQLException e )
        {
            e.printStackTrace();
        }

        return viewersName;

    }

    @Override
    public boolean isViewTableExists( String viewTableName )
    {
        try
        {
            DatabaseMetaData mtdt = jdbcTemplate.getDataSource().getConnection().getMetaData();
            
            ResultSet rs = mtdt.getTables( null, null, viewTableName.toLowerCase(), types );

            return rs.next();
        }
        catch ( Exception e )
        {
            return false;
        }
    }

    @Override
    public String createView( SqlView sqlViewInstance )
    {
        String viewName = sqlViewInstance.getViewName();

        dropViewTable( viewName );

        final String sql = PREFIX_CREATEVIEW_QUERY + viewName + " AS " + sqlViewInstance.getSqlQuery();
        
        log.debug( "Create view SQL: " + sql );
        
        try
        {
            jdbcTemplate.execute( sql );
        }
        catch ( BadSqlGrammarException bge )
        {
            return bge.getCause().getMessage();
        }

        return null;
    }

    @Override
    public void setUpDataSqlViewTable( Grid grid, String viewTableName, Map<String, String> criteria )
    {
        String sql = PREFIX_SELECT_QUERY + viewTableName;
        
        if ( criteria != null && !criteria.isEmpty() )
        {
            SqlHelper helper = new SqlHelper();
            
            for ( String filter : criteria.keySet() )
            {
                sql += " " + helper.whereAnd() + " " + filter + "='" + criteria.get( filter ) + "'";
            }
        }
        
        log.info( "Get view SQL: " + sql );
        
        try
        {
            ResultSet rs = getResultSet( sql );

            grid.addHeaders( rs );
            grid.addRows( rs );
        }
        catch ( SQLException e )
        {
            throw new RuntimeException( "Failed to get data from view " + viewTableName, e );
        }
    }

    @Override
    public String testSqlGrammar( String sql )
    {
        String viewNameCheck = SqlView.PREFIX_VIEWNAME + System.currentTimeMillis();

        sql = PREFIX_CREATEVIEW_QUERY + viewNameCheck + " AS " + sql;
        
        log.debug( "Test view SQL: " + sql );
        
        try
        {
            jdbcTemplate.execute( sql );

            dropViewTable( viewNameCheck );
        }
        catch ( Exception ex )
        {
            return ex.getCause().getMessage();
        }

        return "";
    }

    @Override
    public void dropViewTable( String viewName )
    {
        try
        {
            jdbcTemplate.update( PREFIX_DROPVIEW_QUERY + viewName );
        }
        catch ( Exception ex )
        {
            throw new RuntimeException( "Failed to drop view: " + viewName, ex );
        }
    }

    // -------------------------------------------------------------------------
    // Supporting methods
    // -------------------------------------------------------------------------

    /**
     * Obtains a scrollable, read-only result set based on the query string.
     */
    private ResultSet getResultSet( String sql )
        throws SQLException
    {
        Connection con = jdbcTemplate.getDataSource().getConnection();
        Statement stm = con.createStatement( ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY );
        return stm.executeQuery( sql );
    }
}