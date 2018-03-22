import React from 'react';
import styled, { keyframes } from 'styled-components';

const circleFadeDelay = keyframes`
  0%,
  39%,
  100% {
    opacity: 0;
  }

  40% {
    opacity: 1;
  }
`;

export interface Props {
  delay?: number;
  rotate?: number;
}

const Circle: React.StatelessComponent<Props> = (props) => {
  const CirclePrimitive = styled.div`
    width: 100%;
    height: 100%;
    position: absolute;
    left: 0;
    top: 0;
    transform: rotate(${props.rotate || 0}deg);

    &::before {
      content: '';
      display: block;
      margin: 0 auto;
      width: 15%;
      height: 15%;
      background-color: #999;
      border-radius: 100%;
      animation: ${circleFadeDelay} 1.2s infinite ease-in-out both;
      animation-delay: ${props.delay || 0}s;
    }
  `;
  return <CirclePrimitive />;
};

export default Circle;
