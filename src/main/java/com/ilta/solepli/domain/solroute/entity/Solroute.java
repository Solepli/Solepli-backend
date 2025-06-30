package com.ilta.solepli.domain.solroute.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.ilta.solepli.domain.user.entity.User;
import com.ilta.solepli.global.entity.Timestamped;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "solroutes")
public class Solroute extends Timestamped {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  @OnDelete(action = OnDeleteAction.CASCADE)
  private User user;

  @Column(nullable = false)
  private Integer iconId;

  @Column(nullable = false, length = 25)
  private String name;

  @OneToMany(mappedBy = "solroute", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<SolroutePlace> solroutePlaces = new ArrayList<>();

  @Column(nullable = false)
  @Builder.Default
  private Boolean status = false;

  public void addSolroutePlace(SolroutePlace solroutePlace) {
    solroutePlaces.add(solroutePlace);
    solroutePlace.setSolroute(this);
  }

  public Boolean updateStatus() {
    status = !status;
    return status;
  }

  public void updateInfo(Integer iconId, String name) {
    if (iconId != null && !this.iconId.equals(iconId)) {
      this.iconId = iconId;
    }

    if (name != null && !this.name.equals(name)) {
      this.name = name;
    }
  }
}
