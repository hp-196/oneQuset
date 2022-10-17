package com.oneqst.quest.repository.query;

import com.oneqst.Member.domain.Member;
import com.oneqst.Member.repository.MemberRepository;
import com.oneqst.quest.domain.Quest;
import com.oneqst.quest.dto.MyQuestDto;
import com.oneqst.quest.dto.QMyQuestDto;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.util.StringUtils;

import javax.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;

import static com.oneqst.Member.domain.QMember.member;
import static com.oneqst.quest.domain.QQuest.quest;

public class QuestRepositoryCustomImpl implements QuestRepositoryCustom {
    private final JPAQueryFactory queryFactory;
//    private final EntityManager entityManager;

    @Autowired
    private MemberRepository memberRepository;


    public QuestRepositoryCustomImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
//        this.entityManager = em;
    }

    public static BooleanExpression titleContains(String title) {
        return StringUtils.hasText(title) ? quest.questTitle.contains(title) : null;
    }

    @Override
    public List<Quest> myQuests(Long memberId) {
        return queryFactory
                .selectFrom(quest)
                .join(quest.questMember, member).fetchJoin()
                .where(
                        member.id.eq(memberId))
                .fetch();
    }

    @Override
    public List<Quest> otherQuests(Long memberId) {
        return queryFactory
                .selectFrom(quest)
                .join(quest.questMember, member).fetchJoin()
                .where(member.id.ne(memberId))
                .fetch();
    }

    @Override
    public List<Quest> search(Long memberId, String title) {
        return queryFactory
                .selectFrom(quest)
                .join(quest.questMember, member).fetchJoin()
                .where(
                        member.id.eq(memberId),
                        titleContains(title)
                )
                .fetch();
    }

    @Override
    public List<Quest> questMaster(Long memberId) {
        return queryFactory
                .selectFrom(quest)
                .join(quest.questMaster, member).fetchJoin()
                .where(member.id.eq(memberId))
                .fetch();
    }

    @Override
    public List<Quest> questMember(Long memberId) {
        return queryFactory
                .selectFrom(quest)
                .join(quest.questMaster, member).fetchJoin()
                .where(member.id.eq(memberId))
                .fetch();
    }

    @Override
    public Page<Quest> searchPaging(Member member, String title, Pageable pageable) {
//        QueryResults<Quest> result = queryFactory
//                .selectFrom(quest)
//                .where(quest.questTitle.contains(title)
//                        .or(quest.tags.any().title.contains(title))
//                        .and(quest.questMember.contains(member).not()))
//                .offset(pageable.getOffset())
//                .limit(pageable.getPageSize())
//                .fetchResults();

        List<Quest> content = queryFactory
                .selectFrom(quest)
                .where(quest.questTitle.contains(title)
                        .or(quest.tags.any().title.contains(title))
                        .and(quest.questMember.contains(member).not()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(quest.count())
                .from(quest)
                .where(quest.questTitle.contains(title)
                        .or(quest.tags.any().title.contains(title))
                        .and(quest.questMember.contains(member).not()));

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    /**
     * 참여중 X, 회원 모집중, 진행중인
     * 무작위 퀘스트 9개 추출
     * https://stackoverflow.com/questions/15869279/does-querydsl-not-support-rand
     */
    @Override
    public List<Quest> findRandom9(Long memberId) {
        Member findMember = memberRepository.findById(memberId).orElseThrow(IllegalAccessError::new);
        return queryFactory
                .selectFrom(quest)
                .where(quest.questMember.contains(findMember).not()
                        .and(quest.questRecruitEnd.eq(true))
                        .and(quest.questStartTime.before(LocalDate.now()))
                        .and(quest.questEndTime.after(LocalDate.now())))
                .orderBy(NumberExpression.random().asc())
                .limit(9)
                .fetch();
//        JPAQuery<Quest> query = new JPAQuery<>(entityManager, MySqlJpaTemplates.DEFAULT);
//        QQuest qQuest = new QQuest("quest");
//        return query.
//                from(qQuest)
//                .where(qQuest.questMember.contains(member).not()
//                        .and(qQuest.questRecruitEnd.eq(true))
//                        .and(qQuest.questStartTime.before(LocalDate.now()))
//                        .and(qQuest.questEndTime.after(LocalDate.now())))
//                .orderBy(NumberExpression.random().asc())
//                .limit(9)
//                .fetch();
    }

    /**
     * 나의 퀘스트 활동 목록 조회
     *
     * @param memberId 유저의 Id
     * @return 해당 유저의 퀘스트 활동 리스트
     */
    @Override
    public List<MyQuestDto> myActivityQuestLookup(Long memberId) {
        return queryFactory
                .select(new QMyQuestDto(
                        quest.questUrl,
                        quest.questTitle,
                        quest.questIntroduce,
                        quest.questStartTime,
                        quest.questEndTime
                ))
                .from(quest)
                .join(quest.questMember, member)
                .where(member.id.eq(memberId))
                .orderBy(quest.questStartTime.desc())
                .fetch();
    }
}