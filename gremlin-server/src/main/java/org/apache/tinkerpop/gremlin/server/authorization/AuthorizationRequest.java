/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.tinkerpop.gremlin.server.authorization;

import java.util.Date;
/**
 * Authorization Request Beam
 *
 * @author shekhar.bansal
 */
public class AuthorizationRequest {

  public enum AccessType {
    READ,
    WRITE;
  }

  private final String user;

  private String userGroup;

  private AccessType accessType;

  private String action;

  private Date accessTime;

  private String clientIPAddress;

  private String resource;

  public AuthorizationRequest(String user) {
    this.user = user;
  }

  public String getUser() {
    return user;
  }

  public String getUserGroup() {
    return userGroup;
  }

  public void setUserGroup(String userGroup) {
    this.userGroup = userGroup;
  }

  public AccessType getAccessType() {
    return accessType;
  }

  public void setAccessType(AccessType accessType) {
    this.accessType = accessType;
  }

  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    this.action = action;
  }

  public Date getAccessTime() {
    return accessTime;
  }

  public void setAccessTime(Date accessTime) {
    this.accessTime = accessTime;
  }

  public String getClientIPAddress() {
    return clientIPAddress;
  }

  public void setClientIPAddress(String clientIPAddress) {
    this.clientIPAddress = clientIPAddress;
  }

  public String getResource() {
    return resource;
  }

  public void setResource(String resource) {
    this.resource = resource;
  }

  @Override
  public String toString() {
    return "AuthorizationRequest{" +
            "user='" + user + '\'' +
            ", userGroup='" + userGroup + '\'' +
            ", accessType='" + accessType + '\'' +
            ", action='" + action + '\'' +
            ", accessTime='" + accessTime + '\'' +
            ", clientIPAddress='" + clientIPAddress + '\'' +
            ", resource='" + resource + '\'' +
            '}';
  }

}
