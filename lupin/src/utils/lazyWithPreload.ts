import { lazy, LazyExoticComponent } from "react";

// React.lazy와 동일한 타입 시그니처 사용
type LazyComponentWithPreload<T extends React.ComponentType<never>> = LazyExoticComponent<T> & {
  preload: () => Promise<{ default: T }>;
};

/**
 * lazy + preload 기능을 가진 동적 import 유틸리티
 * 마우스 hover 시 미리 다운로드를 시작하여 클릭 시 즉시 렌더링 가능
 */
export function lazyWithPreload<T extends React.ComponentType<never>>(
  factory: () => Promise<{ default: T }>
): LazyComponentWithPreload<T> {
  const Component = lazy(factory) as LazyComponentWithPreload<T>;
  Component.preload = factory;
  return Component;
}
