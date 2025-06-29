package com.ilta.solepli.domain.solmark.place.service;

import java.util.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.ilta.solepli.domain.place.entity.Place;
import com.ilta.solepli.domain.place.repository.PlaceRepository;
import com.ilta.solepli.domain.solmark.place.dto.CollectionCountDto;
import com.ilta.solepli.domain.solmark.place.dto.reqeust.*;
import com.ilta.solepli.domain.solmark.place.dto.response.CollectionResponse;
import com.ilta.solepli.domain.solmark.place.dto.response.SolmarkPlaceDto;
import com.ilta.solepli.domain.solmark.place.dto.response.SolmarkPlacesResponse;
import com.ilta.solepli.domain.solmark.place.entity.SolmarkPlace;
import com.ilta.solepli.domain.solmark.place.entity.SolmarkPlaceCollection;
import com.ilta.solepli.domain.solmark.place.repository.SolmarkPlaceCollectionRepository;
import com.ilta.solepli.domain.solmark.place.repository.SolmarkPlaceRepository;
import com.ilta.solepli.domain.user.entity.User;
import com.ilta.solepli.domain.user.util.CustomUserDetails;
import com.ilta.solepli.global.entity.Timestamped;
import com.ilta.solepli.global.exception.CustomException;
import com.ilta.solepli.global.exception.ErrorCode;
import com.ilta.solepli.global.util.PlaceUtil;

@Slf4j
@Service
@RequiredArgsConstructor
public class SolmarkPlaceService {

  private final SolmarkPlaceCollectionRepository solmarkPlaceCollectionRepository;
  private final PlaceRepository placeRepository;
  private final SolmarkPlaceRepository solmarkPlaceRepository;

  private static final int MAX_PLACES_PER_COLLECTION = 100;
  private static final int MAX_COLLECTIONS_PER_USER = 50;
  private static final int TAG_LIMIT = 3;

  @Transactional
  public void createCollection(
      CustomUserDetails customUserDetails, CreateCollectionRequest createCollectionRequest) {

    User user = customUserDetails.user();

    // 사용자 저장 리스트 개수 조회
    Integer count = solmarkPlaceCollectionRepository.countByUser(user);

    // 최대 개수(50) 검증
    if (count >= MAX_COLLECTIONS_PER_USER) {
      throw new CustomException(ErrorCode.COLLECTION_LIMIT_EXCEEDED);
    }

    // 쏠마크 저장 리스트 객체 생성
    SolmarkPlaceCollection placeCollection =
        SolmarkPlaceCollection.builder()
            .user(user)
            .name(createCollectionRequest.collectionName())
            .iconId(createCollectionRequest.iconId())
            .build();

    solmarkPlaceCollectionRepository.save(placeCollection);
  }

  @Transactional(readOnly = true)
  public List<CollectionResponse> getCollections(CustomUserDetails customUserDetails) {
    User user = customUserDetails.user();

    // 사용자 쏠마크 -장소 저장 리스트 조회
    List<SolmarkPlaceCollection> solmarkPlaceCollections =
        solmarkPlaceCollectionRepository.findByUser(user);

    return solmarkPlaceCollections.stream()
        // CollectionResponse DTO 매핑
        .map(this::mapToCollectionResponse)
        .toList();
  }

  private CollectionResponse mapToCollectionResponse(SolmarkPlaceCollection spc) {
    int placeCount = spc.getSolmarkPlaces().size();

    return CollectionResponse.builder()
        .collectionId(spc.getId())
        .iconId(spc.getIconId())
        .collectionName(spc.getName())
        .placeCount(placeCount)
        .build();
  }

  @Transactional(readOnly = true)
  public SolmarkPlacesResponse getSolmarkPlaces(
      CustomUserDetails customUserDetails, Long collectionId) {

    // 특정 쏠마크 장소 저장 리스트(collectionId) 조회
    List<SolmarkPlace> solmarkplaces =
        solmarkPlaceRepository.findByUserAndCollectionId(customUserDetails.user(), collectionId);
    // 쏠마크 장소 개수
    int placeCount = solmarkplaces.size();

    // 쏠마크 장소 조회 DTO 매핑
    List<SolmarkPlaceDto> solmarkPlaceDtos =
        solmarkplaces.stream().map(this::mapToSolmarkPlaceDto).toList();

    return SolmarkPlacesResponse.of(solmarkPlaceDtos, placeCount);
  }

  private SolmarkPlaceDto mapToSolmarkPlaceDto(SolmarkPlace sp) {
    Long placeId = sp.getPlace().getId();
    String name = sp.getPlace().getName();
    String detailedCategory = sp.getPlace().getTypes();
    Integer recommendationPercent = placeRepository.getRecommendationPercent(placeId);
    List<String> tags = placeRepository.getTopTagsForPlace(placeId, TAG_LIMIT);
    Double rating = PlaceUtil.truncateTo2Decimals(sp.getPlace().getRating());

    return SolmarkPlaceDto.builder()
        .PlaceId(placeId)
        .name(name)
        .detailedCategory(detailedCategory)
        .recommendationPercent(recommendationPercent)
        .tags(tags)
        .rating(rating)
        .build();
  }

  @Transactional
  public void updateCollection(
      CustomUserDetails customUserDetails,
      Long collectionId,
      UpdateCollectionRequest updateCollectionRequest) {
    User user = customUserDetails.user();

    // 사용자의 쏠마크 저장 리스트 조회
    SolmarkPlaceCollection solmarkPlaceCollection =
        solmarkPlaceCollectionRepository
            .findByIdAndUser(collectionId, user)
            .orElseThrow(() -> new CustomException(ErrorCode.COLLECTION_NOT_FOUND));

    // 수정할 저장 리스트 이름과 아이콘 번호
    String updateName = updateCollectionRequest.collectionName();
    Integer updateIconId = updateCollectionRequest.iconId();

    // 쏠마크 저장 리스트 수정
    solmarkPlaceCollection.updateInfo(updateName, updateIconId);
  }

  @Transactional
  public void deleteCollection(CustomUserDetails customUserDetails, Long collectionId) {
    User user = customUserDetails.user();

    // 쏠마크 저장 리스트 삭제
    solmarkPlaceCollectionRepository
        .findByIdAndUser(collectionId, user)
        .orElseThrow(() -> new CustomException(ErrorCode.COLLECTION_NOT_FOUND))
        .softDelete(); // delete 시점 기록
  }

  @Transactional
  public void updatePlaceCollections(
      CustomUserDetails customUserDetails, Long placeId, UpdatePlaceCollectionsRequest req) {
    User user = customUserDetails.user();

    // 장소 조회
    Place place =
        placeRepository
            .findById(placeId)
            .orElseThrow(() -> new CustomException(ErrorCode.PLACE_NOT_EXISTS));

    // 추가/삭제할 저장 리스트 ID 리스트 (null 방지)
    List<Long> addCollectionIds =
        req.addCollectionIds() == null ? List.of() : req.addCollectionIds();
    List<Long> removeCollectionIds =
        req.removeCollectionIds() == null ? List.of() : req.removeCollectionIds();

    // 현재 사용자가 해당 장소를 마크한 저장 리스트 ID Set 조회
    Set<Long> existingIdSet =
        new HashSet<>(solmarkPlaceCollectionRepository.findByUserAndPlaceId(user, placeId));

    // 실제로 추가해야 할 저장 리스트 ID 리스트
    List<Long> toAdd = filterToAdd(addCollectionIds, existingIdSet);
    // 실제로 soft-delete 해야 할 저장 리스트 ID 리스트
    List<Long> toRemove = filterToRemove(removeCollectionIds, existingIdSet);

    // 저장 리스트별 장소 개수 제한 검증
    validateCollectionPlaceLimit(toAdd);

    // 쏠마크 장소 추가
    addSolmarkPlace(user, toAdd, place);
    // 쏠마크 장소 삭제(softDelete)
    softDeleteSolmarkPlace(user, toRemove, place);
  }

  /** 지정한 저장 리스트에서 해당 장소에 대한 쏠마크를 soft-delete 처리 */
  private void softDeleteSolmarkPlace(User user, List<Long> toRemove, Place place) {
    if (toRemove.isEmpty()) return;
    List<SolmarkPlaceCollection> removeCollections =
        solmarkPlaceCollectionRepository.findByUserAndIdInAndDeletedAtIsNullWithPlaces(
            user, toRemove);

    removeCollections.forEach(
        c -> {
          c.getSolmarkPlaces().stream()
              .filter(sp -> sp.getPlace().equals(place) && sp.getDeletedAt() == null)
              .forEach(Timestamped::softDelete);
        });
  }

  /** 지정한 저장 리스트에 해당 장소를 쏠마크로 추가 */
  private void addSolmarkPlace(User user, List<Long> toAdd, Place place) {
    if (toAdd.isEmpty()) return;
    List<SolmarkPlaceCollection> addCollections =
        solmarkPlaceCollectionRepository.findByUserAndIdInAndDeletedAtIsNull(user, toAdd);

    List<SolmarkPlace> addSolmarkPlaces =
        addCollections.stream()
            .map(c -> SolmarkPlace.builder().solmarkPlaceCollection(c).place(place).build())
            .toList();

    solmarkPlaceRepository.saveAll(addSolmarkPlaces);
  }

  /** 저장 리스트별 장소 개수 제한 검증 (최대 개수 초과 시 예외 발생) */
  private void validateCollectionPlaceLimit(List<Long> toAdd) {
    List<CollectionCountDto> collectionCountDtos =
        solmarkPlaceRepository.countByCollectionIds(toAdd);

    collectionCountDtos.forEach(
        dto -> {
          if (dto.count() >= MAX_PLACES_PER_COLLECTION) {
            throw new CustomException(ErrorCode.EXCEEDED_MARK_PLACE_LIMIT);
          }
        });
  }

  /** 삭제 대상 저장 리스트 ID만 필터링 */
  private List<Long> filterToRemove(List<Long> removeCollectionIds, Set<Long> existingIdSet) {
    return removeCollectionIds.stream().filter(existingIdSet::contains).toList();
  }

  /** 추가 대상 저장 리스트 ID만 필터링 */
  private List<Long> filterToAdd(List<Long> addCollectionIds, Set<Long> existingIdSet) {
    return addCollectionIds.stream().filter(id -> !existingIdSet.contains(id)).toList();
  }
}
