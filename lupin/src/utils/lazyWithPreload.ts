import { lazy, ComponentType, LazyExoticComponent } from "react";

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type AnyProps = Record<string, any>;

type LazyComponentWithPreload<T extends ComponentType<AnyProps>> = LazyExoticComponent<T> & {
  preload: () => Promise<{ default: T }>;
};

/**
 * lazy + preload 기능을 가진 동적 import 유틸리티
 * 마우스 hover 시 미리 다운로드를 시작하여 클릭 시 즉시 렌더링 가능
 */
export function lazyWithPreload<T extends ComponentType<AnyProps>>(
  factory: () => Promise<{ default: T }>
): LazyComponentWithPreload<T> {
  const Component = lazy(factory) as LazyComponentWithPreload<T>;
  Component.preload = factory;
  return Component;
}
