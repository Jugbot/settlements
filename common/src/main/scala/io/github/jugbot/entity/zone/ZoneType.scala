package io.github.jugbot.entity.zone

/**
 * Type of the zone which restricts the structure of the zone tree.
 * - A zone type contains information on validChildren and its inverse, validParents.
 * - A zone must be contained inside one of its valid parents, if any
 * - A zone of type cannot overlap other zones of the same type
 */
enum ZoneType(validChildrenSupplier: () => Set[ZoneType]):
  case Settlement extends ZoneType(() => Set(ZoneType.Zoning, ZoneType.Structure))
  case Zoning extends ZoneType(() => Set(ZoneType.Structure))
  case Structure extends ZoneType(() => Set.empty[ZoneType])

  lazy val validChildren: Set[ZoneType] = validChildrenSupplier()
  lazy val validParents: Set[ZoneType] = findParents(this)

private def findParents(target: ZoneType, root: ZoneType = ZoneType.Settlement): Set[ZoneType] =
  def loop(current: ZoneType, path: Set[ZoneType] = Set.empty): Set[ZoneType] =
    if current == target then path
    else if !path.contains(current) then current.validChildren.flatMap(loop(_, path + current))
    else Set.empty
  loop(root)
