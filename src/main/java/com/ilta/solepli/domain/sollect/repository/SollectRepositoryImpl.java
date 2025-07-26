package com.ilta.solepli.domain.sollect.repository;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import com.ilta.solepli.domain.category.entity.QCategory;
import com.ilta.solepli.domain.place.entity.QPlace;
import com.ilta.solepli.domain.place.entity.QPlaceCategory;
import com.ilta.solepli.domain.sollect.dto.PopularSollectResponseContent;
import com.ilta.solepli.domain.sollect.dto.QPopularSollectResponseContent;
import com.ilta.solepli.domain.sollect.dto.QSollectSearchResponseContent;
import com.ilta.solepli.domain.sollect.dto.SollectSearchResponseContent;
import com.ilta.solepli.domain.sollect.entity.ContentType;
import com.ilta.solepli.domain.sollect.entity.QSollect;
import com.ilta.solepli.domain.sollect.entity.QSollectContent;
import com.ilta.solepli.domain.sollect.entity.mapping.QSollectPlace;
import com.ilta.solepli.domain.solmark.sollect.dto.response.QSolmarkSollectResponseContent;
import com.ilta.solepli.domain.solmark.sollect.dto.response.SolmarkSollectResponseContent;
import com.ilta.solepli.domain.solmark.sollect.entity.QSolmarkSollect;

@RequiredArgsConstructor
public class SollectRepositoryImpl implements SollectRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  QSollect sollect = QSollect.sollect;
  QSollectPlace sollectPlace = QSollectPlace.sollectPlace;
  QSollectContent sollectContent = QSollectContent.sollectContent;
  QPlace place = QPlace.place;
  QPlaceCategory placeCategory = QPlaceCategory.placeCategory;
  QCategory category = QCategory.category;
  QSolmarkSollect solmarkSollect = QSolmarkSollect.solmarkSollect;

  @Override
  public List<SollectSearchResponseContent> searchSollectByKeywordOrCategory(
      Long cursorId, int size, String parsedKeyword, String parsedCategory) {

    // 1. 키워드 또는 카테고리에 해당하는 Place ID 추출
    List<Long> placeIds =
        queryFactory
            .select(place.id)
            .from(place)
            .leftJoin(place.placeCategories, placeCategory)
            .leftJoin(placeCategory.category, category)
            .where(anyMatchKeyword(parsedKeyword), matchCategory(parsedCategory))
            .fetch();

    BooleanBuilder sollectCondition = new BooleanBuilder();
    BooleanExpression titleCond = matchSollectTitle(parsedKeyword);
    BooleanExpression placeCond = placeIds.isEmpty() ? null : sollectPlace.place.id.in(placeIds);
    if (titleCond != null) sollectCondition.or(titleCond);
    if (placeCond != null) sollectCondition.or(placeCond);

    // 커서 조건 추가
    BooleanExpression cursorCondition = cursorLessThan(cursorId);
    if (cursorCondition != null) sollectCondition.and(cursorCondition);

    sollectCondition.and(sollect.deletedAt.isNull());

    // ID 추출
    List<Long> sollectIds =
        queryFactory
            .select(sollect.id)
            .from(sollect)
            .leftJoin(sollect.sollectPlaces, sollectPlace)
            .where(sollectCondition)
            .distinct()
            .orderBy(sollect.id.desc())
            .limit(size + 1)
            .fetch();

    // DTO 반환
    QSollectPlace firstPlace = new QSollectPlace("firstPlace");
    QPlace firstPlaceInfo = new QPlace("firstPlaceInfo");

    return queryFactory
        .select(
            new QSollectSearchResponseContent(
                sollect.id,
                sollectContent.imageUrl,
                sollect.title,
                firstPlaceInfo.district,
                firstPlaceInfo.neighborhood))
        .from(sollect)
        .join(sollect.sollectPlaces, firstPlace)
        .on(firstPlace.seq.eq(0))
        .join(firstPlace.place, firstPlaceInfo)
        .join(sollect.sollectContents, sollectContent)
        .on(sollectContent.seq.eq(0L))
        .where(sollect.id.in(sollectIds), sollect.deletedAt.isNull())
        .orderBy(sollect.id.desc())
        .fetch();
  }

  @Override
  public List<SolmarkSollectResponseContent> searchSolmarkSollectBySollectIdsAndCursor(
      Long cursorId, int size, List<Long> sollectIds) {
    QSollectPlace firstPlace = new QSollectPlace("firstPlace");
    QPlace firstPlaceInfo = new QPlace("firstPlaceInfo");

    // 커서 조건 추가
    BooleanExpression cursorCondition = cursorLessThan(cursorId);

    return queryFactory
        .select(
            new QSolmarkSollectResponseContent(
                sollect.id,
                sollectContent.imageUrl,
                sollect.title,
                firstPlaceInfo.district,
                firstPlaceInfo.neighborhood))
        .from(sollect)
        .join(sollect.sollectPlaces, firstPlace)
        .on(firstPlace.seq.eq(0))
        .join(firstPlace.place, firstPlaceInfo)
        .join(sollect.sollectContents, sollectContent)
        .on(sollectContent.seq.eq(0L))
        .where(sollect.id.in(sollectIds), sollect.deletedAt.isNull(), cursorCondition)
        .orderBy(sollect.id.desc())
        .limit(size + 1)
        .fetch();
  }

  @Override
  public List<PopularSollectResponseContent> searchSollectBySollectIds(List<Long> sollectIds) {
    QSollectPlace firstPlace = new QSollectPlace("firstPlace");
    QPlace firstPlaceInfo = new QPlace("firstPlaceInfo");

    return queryFactory
        .select(
            new QPopularSollectResponseContent(
                sollect.id,
                sollectContent.imageUrl,
                sollect.title,
                firstPlaceInfo.name,
                firstPlaceInfo.district,
                firstPlaceInfo.neighborhood))
        .from(sollect)
        .join(sollect.sollectPlaces, firstPlace)
        .on(firstPlace.seq.eq(0))
        .join(firstPlace.place, firstPlaceInfo)
        .join(sollect.sollectContents, sollectContent)
        .on(sollectContent.seq.eq(0L))
        .where(sollect.id.in(sollectIds), sollect.deletedAt.isNull())
        .fetch();
  }

  @Override
  public List<SollectSearchResponseContent> searchSollectBySollectIdsAndCursor(
      Long cursorId, int size, List<Long> sollectIds) {

    BooleanBuilder sollectCondition = new BooleanBuilder();
    BooleanExpression cursorCondition = cursorLessThan(cursorId);
    if (cursorCondition != null) sollectCondition.and(cursorCondition);

    sollectCondition.and(sollect.id.in(sollectIds));
    sollectCondition.and(sollect.deletedAt.isNull());

    // DTO 반환
    QSollectPlace firstPlace = new QSollectPlace("firstPlace");
    QPlace firstPlaceInfo = new QPlace("firstPlaceInfo");

    return queryFactory
        .select(
            new QSollectSearchResponseContent(
                sollect.id,
                sollectContent.imageUrl,
                sollect.title,
                firstPlaceInfo.district,
                firstPlaceInfo.neighborhood))
        .from(sollect)
        .join(sollect.sollectPlaces, firstPlace)
        .on(firstPlace.seq.eq(0))
        .join(firstPlace.place, firstPlaceInfo)
        .join(sollect.sollectContents, sollectContent)
        .on(sollectContent.seq.eq(0L))
        .where(sollectCondition)
        .orderBy(sollect.id.desc())
        .limit(size + 1)
        .fetch();
  }

  @Override
  public List<SollectSearchResponseContent> searchRecommendSollectByKeywordOrCategory(
      String keyword, String categoryName) {
    // 카테고리에 해당하는 Place ID 추출
    List<Long> placeIds =
        queryFactory
            .select(place.id)
            .from(place)
            .leftJoin(place.placeCategories, placeCategory)
            .leftJoin(placeCategory.category, category)
            .where(matchCategory(categoryName))
            .fetch();

    BooleanBuilder condition = new BooleanBuilder();

    if (!placeIds.isEmpty()) condition.and(sollectPlace.place.id.in(placeIds));

    // 선택 키워드 조건 (Sollect.title 또는 SollectContent.text(TEXT 타입만))
    if (keyword != null && !keyword.isBlank()) {
      condition.and(matchSollectTitle(keyword).or(matchSollectText(keyword)));
    }

    QSolmarkSollect solmark = QSolmarkSollect.solmarkSollect;

    // 해당 조건에 해당하고, 쏠마크가 많은 쏠렉트 아이디 8개 추출
    List<Long> sollectIds =
        queryFactory
            .select(sollect.id)
            .from(sollect)
            .leftJoin(sollect.sollectContents, sollectContent)
            .leftJoin(sollect.sollectPlaces, sollectPlace)
            .leftJoin(sollectPlace.place, place)
            .leftJoin(solmark)
            .on(solmark.sollect.eq(sollect))
            .where(condition, sollect.deletedAt.isNull())
            .groupBy(sollect.id)
            .orderBy(solmark.id.count().desc())
            .limit(8)
            .fetch();

    Map<Long, Integer> orderMap = new HashMap<>();
    for (int i = 0; i < sollectIds.size(); i++) {
      orderMap.put(sollectIds.get(i), i); // ID를 key로, 순서를 value로 저장
    }

    QSollectPlace firstPlace = new QSollectPlace("firstPlace");
    QPlace firstPlaceInfo = new QPlace("firstPlaceInfo");

    // DTO 반환
    List<SollectSearchResponseContent> results =
        queryFactory
            .select(
                new QSollectSearchResponseContent(
                    sollect.id,
                    sollectContent.imageUrl,
                    sollect.title,
                    firstPlaceInfo.district,
                    firstPlaceInfo.neighborhood))
            .from(sollect)
            .join(sollect.sollectPlaces, firstPlace)
            .on(firstPlace.seq.eq(0))
            .join(firstPlace.place, firstPlaceInfo)
            .join(sollect.sollectContents, sollectContent)
            .on(sollectContent.seq.eq(0L))
            .where(sollect.id.in(sollectIds))
            .orderBy(sollect.id.desc())
            .fetch();
    results.sort(Comparator.comparingInt(dto -> orderMap.get(dto.sollectId())));
    return results;
  }

  private BooleanExpression anyMatchKeyword(String keyword) {
    if (keyword == null || keyword.isBlank()) return null;
    return place
        .address
        .containsIgnoreCase(keyword)
        .or(place.district.containsIgnoreCase(keyword))
        .or(place.neighborhood.containsIgnoreCase(keyword));
  }

  private BooleanExpression matchCategory(String categoryName) {
    if (categoryName == null || categoryName.isBlank()) return null;
    return category.name.eq(categoryName);
  }

  private BooleanExpression matchSollectTitle(String keyword) {
    if (keyword == null || keyword.isBlank()) return null;
    return sollect.title.containsIgnoreCase(keyword);
  }

  private BooleanExpression cursorLessThan(Long cursorId) {
    return (cursorId != null) ? sollect.id.lt(cursorId) : null;
  }

  private BooleanExpression matchSollectText(String keyword) {
    if (keyword == null || keyword.isBlank()) return null;
    return sollectContent
        .type
        .eq(ContentType.TEXT)
        .and(sollectContent.text.stringValue().like("%" + keyword + "%"));
  }
}
