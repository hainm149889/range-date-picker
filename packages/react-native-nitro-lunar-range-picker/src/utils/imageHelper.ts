import { Image, type ImageSourcePropType } from "react-native";

export function getNitroImageUri(
  source?: ImageSourcePropType | string,
): string | undefined {
  if (!source) return undefined;

  // Nếu là chuỗi URL hoặc file path string
  if (typeof source === "string") {
    return source;
  }

  // Nếu là local asset qua require(...)
  const resolvedAsset = Image.resolveAssetSource(source);
  return resolvedAsset ? resolvedAsset.uri : undefined;
}
