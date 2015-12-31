/*
 * Copyright (c) 2003-2015 BzzAgent, Inc. All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * BzzAgent, Inc. ("Confidential Information"). You shall not disclose
 * such Confidential Information and shall use it only in accordance
 * with the terms of the license agreement you entered into with BzzAgent.
 *
 * BzzAgent MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY
 * OF THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
 * TO THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE, OR NON-INFRINGEMENT. SUN SHALL NOT BE LIABLE FOR ANY DAMAGES
 * SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING
 * THIS SOFTWARE OR ITS DERIVATIVES.
 * 
 */

package bzzAgent;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.apache.commons.lang3.Validate;

import com.bzzagent.bo.CampaignInviteStatus;
import com.bzzagent.common.enums.BsbActivityStatus;
import com.bzzagent.common.service.bitesized.BiteSizedActivitySearchParams;
import com.bzzagent.common.service.bitesized.BsbAgentActivitySearchParams;
import com.bzzagent.common.service.bitesized.BsbAgentActivitySearchParams.SortOrder;
import com.bzzagent.domain.BiteSizedActivityEntity;
import com.bzzagent.domain.BsbAgentActivityEntity;
import com.bzzagent.webapp.util.BzzUtil;

/**
 * JPA {@link BiteSizedBzzDao} implementation.
 * 
 * @author lirazg
 */
public class BiteSizedBzzDaoJpa implements BiteSizedBzzDao {

    private final EntityManager em;
    
    public BiteSizedBzzDaoJpa(EntityManager em) {
        this.em = em;
    }
    
    @Override
    public BiteSizedActivityEntity getByIdent(String activityIdent) {
        
        final TypedQuery<BiteSizedActivityEntity> result = em.createQuery( 
        		" select bsb from BiteSizedActivityEntity bsb" +
        		//" join fetch bsb.bzzActivityEntity ba " +
				" left join fetch bsb.incentives incentive " +
				" join fetch bsb.campaignEntity c " +
				" join fetch c.campaignPermissionsEntity cp " +
				" left join fetch c.campaignProfiles profiles " +
        		" where bsb.activityIdent=:ident", BiteSizedActivityEntity.class).setParameter("ident", activityIdent);
        return result.getSingleResult();
    }

    @Override
    public BiteSizedActivityEntity getById(Integer bzzactivityId) {
    	String sql =
			"select bsb from BiteSizedActivityEntity bsb" +
			" left join fetch bsb.incentives incentive " +
			" join fetch bsb.campaignEntity c " +
			" join fetch c.campaignPermissionsEntity cp " +
			" left join fetch c.campaignProfiles profiles " +
			" left join fetch profiles.memberGroupEntity mg " +
			"where bsb.bzzActivityEntity.id = :id";

        final TypedQuery<BiteSizedActivityEntity> result = em.createQuery(sql, BiteSizedActivityEntity.class)
        		.setParameter("id", bzzactivityId);
        return result.getSingleResult();
    }

    @Override
    public List<BiteSizedActivityEntity> search(BiteSizedActivitySearchParams params) {
		final LocalDate today =  LocalDate.now();
    	
        String activeQuery;
        if (params.isActive() != null) {
            if (params.isActive()) {
                activeQuery = "where bsae.endDate >= ? "
                			+ " or bsae.phaseInt < 6 ";  //Launch(6, "General Launch")
                
            } else {
                activeQuery = "where bsae.endDate < ? "
                			+ " and bsae.phaseInt = 6 ";
            }
        } else {
            activeQuery = "";
        }
        
        String sql =
            "from BiteSizedActivityEntity bsae " + 
            " left outer join fetch bsae.incentives incentive " +
                    activeQuery +
            		" order by bsae.startDate ASC"
                    ;
        
        //sql += "order by ma.mam.id asc";
        TypedQuery<BiteSizedActivityEntity> query2 = em.createQuery(sql, BiteSizedActivityEntity.class)
            .setFirstResult(params.getOffset())
            //.setMaxResults(params.getLimit())
            ;
        if (params.isActive() != null) {
            query2.setParameter(1, today);
        }
        return query2.getResultList();
    }

	@Override
	public boolean memberHasBSBActivities(String username) {
		 Validate.notBlank(username, "username was missing");
	     Instant cutoff = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();

	      final StringBuilder sql = new StringBuilder("from BsbAgentActivityEntity bsbaae ")
          .append(" left join fetch bsbaae.incentives incentive ")
          .append(" where bsbaae.username = :username ")
          .append(" and bsbaae.campaignInviteStatus in (1,2)") //'New', 'Viewed'
          .append(" and bsbaae.startDate < '"+  cutoff + "' and bsbaae.endDate > '"+cutoff +"'");
      
      final TypedQuery<BsbAgentActivityEntity> query2 = em.createQuery(sql.toString(), BsbAgentActivityEntity.class)
              .setParameter("username", username);
      
      return BzzUtil.isNotEmpty(query2.getResultList());
	}

    
    @Override
    public List<BsbAgentActivityEntity> getBsbAgentActivities(BsbAgentActivitySearchParams params) {
        Validate.notBlank(params.getUsername(), "username was missing");
        Validate.isTrue(params.getStatusList().size() > 0, "no statuses were passed");
        
        final String orderString;
        final String sortOrder = params.getSortorder().toString();
        switch (params.getOrderby()) {
        case lastmodified:
            orderString = " bsbaae.lastModified " + sortOrder;
            break;
        case priority:
            if (params.getSortorder() == SortOrder.asc) {
                orderString = " bsbaae.clientSponsored asc, bsbaae.endDate desc";
            } else {
                orderString = " bsbaae.clientSponsored desc, bsbaae.endDate asc";
            }
            break;
        default:
            throw new RuntimeException("unhandled case for BsbAgentActivitySearchParams.orderBy=" + params.getOrderby());
        }
        final List<Integer> statusList = new ArrayList<>();
        for (BsbActivityStatus bsbActivityStatus : params.getStatusList()) {
            final CampaignInviteStatus campaignInviteStatus = BsbActivityStatus.toCampaignInviteStatus(bsbActivityStatus);
            if (campaignInviteStatus != null) {
                statusList.add(campaignInviteStatus.getValue());
            }
        }
        
        final LocalDate today = LocalDate.now();
        final StringBuilder sql = new StringBuilder("from BsbAgentActivityEntity bsbaae ")
            .append(" left join fetch bsbaae.incentives incentive ")
            .append(" left join fetch bsbaae.ecommerceRetailers ecommerceRetailers ")
            .append(" where bsbaae.username = :username ")
            .append(" and bsbaae.campaignInviteStatus in :statusList")
            .append(" and bsbaae.startDate <= :today and bsbaae.endDate >= :today")
            .append(" order by ").append(orderString);
        
        final TypedQuery<BsbAgentActivityEntity> query2 = em.createQuery(sql.toString(), BsbAgentActivityEntity.class)
                .setParameter("username", params.getUsername())
                .setParameter("today", today)
                .setParameter("statusList", statusList);
        if (params.getLimit()!= null && params.getLimit() > 0) {
            query2.setMaxResults(params.getLimit());
        }
        return query2.getResultList();
    }

	
}
