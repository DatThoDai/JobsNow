package com.JobsNow.backend.entity;

import com.JobsNow.backend.entity.enums.CompanyImageType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer imageId;

    @ManyToOne
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    private String imageUrl;

    @Enumerated(EnumType.STRING)
    private CompanyImageType imageType;
}
