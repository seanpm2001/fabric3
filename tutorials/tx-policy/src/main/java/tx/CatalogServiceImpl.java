/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */
package tx;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.osoa.sca.annotations.EagerInit;
import org.osoa.sca.annotations.Init;
import org.osoa.sca.annotations.Property;

/**
 * @version $Revision$ $Date$
 */
@EagerInit
public class CatalogServiceImpl implements CatalogService {
    
    private static final String INSERT = "INSERT INTO PRODUCT (NAME, DESCRIPTION) VALUES (?, ?)";
    private static final String CREATE = "CREATE TABLE PRODUCT (NAME VARCHAR(30), DESCRIPTION VARCHAR(100)";
    
    private String dataSourceName;
    
    private DataSource dataSource;
    
    @Property(required = true)
    public void setDataSourceName(String dataSourceName) {
        this.dataSourceName = dataSourceName;
    }
    
    @Init
    public void init() throws NamingException, SQLException {
        
        Context ic = null;
        try {
            ic = new InitialContext();
            dataSource = (DataSource) ic.lookup(dataSourceName);
        } finally {
            if(ic != null) {
                ic.close();
            }
        }
        
        Connection con = null;
        Statement stmt = null;
        
        try {
            con = dataSource.getConnection();
            stmt = con.createStatement();
            stmt.executeUpdate(CREATE);
        } finally {
            try {
                if(stmt != null) {
                    stmt.close();
                } 
            } finally {
                if(con != null) {
                    con.close();
                }
            }
        }
        
    }

    public void addProduct(String name, String description) throws SQLException {
        
        Connection con = null;
        PreparedStatement stmt = null;
        
        try {
            
            con = dataSource.getConnection();
            stmt = con.prepareStatement(INSERT);
            
            stmt.setString(1, name);
            stmt.setString(2, name);
            
            stmt.executeUpdate(CREATE);
            
        } finally {
            try {
                if(stmt != null) {
                    stmt.close();
                } 
            } finally {
                if(con != null) {
                    con.close();
                }
            }
        }

    }

}
