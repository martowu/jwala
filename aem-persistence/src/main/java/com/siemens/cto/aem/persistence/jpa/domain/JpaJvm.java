package com.siemens.cto.aem.persistence.jpa.domain;

import java.util.List;

import javax.persistence.*;

import com.siemens.cto.aem.domain.model.jvm.Jvm;

@Entity
@Table(name = "jvm", uniqueConstraints = {@UniqueConstraint(columnNames = {"name"})})
public class JpaJvm extends AbstractEntity<JpaJvm, Jvm> {

    private static final long serialVersionUID = 2491659292018543404L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String hostName;

    @ManyToMany(mappedBy = "jvms")
    private List<JpaGroup> groups;

    @Column(nullable = false)
    private Integer httpPort;

    private Integer httpsPort;

    private Integer redirectPort;

    private Integer shutdownPort;

    private Integer ajpPort;

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public List<JpaGroup> getGroups() {
        return groups;
    }

    public void setGroups(final List<JpaGroup> groups) {
        this.groups = groups;
    }

    public Integer getHttpPort() {
        return httpPort;
    }

    public void setHttpPort(Integer httpPort) {
        this.httpPort = httpPort;
    }

    public Integer getHttpsPort() {
        return httpsPort;
    }

    public void setHttpsPort(Integer httpsPort) {
        this.httpsPort = httpsPort;
    }

    public Integer getRedirectPort() {
        return redirectPort;
    }

    public void setRedirectPort(Integer redirectPort) {
        this.redirectPort = redirectPort;
    }

    public Integer getShutdownPort() {
        return shutdownPort;
    }

    public void setShutdownPort(Integer shutdownPort) {
        this.shutdownPort = shutdownPort;
    }

    public Integer getAjpPort() {
        return ajpPort;
    }

    public void setAjpPort(Integer ajpPort) {
        this.ajpPort = ajpPort;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final JpaJvm jpaJvm = (JpaJvm) o;

        if (id != null ? !id.equals(jpaJvm.id) : jpaJvm.id != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}